package com.pcc.property.service;

import com.pcc.property.dto.request.RentalAgreementRequest;
import com.pcc.property.dto.response.HistoryEntryResponse;
import com.pcc.property.dto.response.PageResponse;
import com.pcc.property.dto.response.RentScheduleResponse;
import com.pcc.property.dto.response.RentalAgreementResponse;
import com.pcc.property.entity.*;
import com.pcc.property.enums.*;
import com.pcc.property.exception.BusinessRuleException;
import com.pcc.property.exception.ResourceNotFoundException;
import com.pcc.property.repository.*;
import com.pcc.property.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import static com.pcc.property.service.PropertyService.actor;

@Service
@RequiredArgsConstructor
public class RentalService {

    private static final EnumSet<AgreementStatus> BLOCKING_STATUSES =
            EnumSet.of(AgreementStatus.DRAFT, AgreementStatus.ACTIVE);

    private final RentalAgreementRepository agreementRepository;
    private final RentScheduleRepository scheduleRepository;
    private final RentalAgreementHistoryRepository historyRepository;
    private final ApartmentRepository apartmentRepository;
    private final TenantRepository tenantRepository;

    @Transactional
    public RentalAgreementResponse create(RentalAgreementRequest req) {
        if (!req.endDate().isAfter(req.startDate())) {
            throw new BusinessRuleException("End date must be after start date");
        }
        Apartment apartment = apartmentRepository.findById(req.apartmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Apartment not found: " + req.apartmentId()));
        Tenant tenant = tenantRepository.findById(req.tenantId())
                .orElseThrow(() -> new ResourceNotFoundException("Tenant not found: " + req.tenantId()));
        if (tenant.getStatus() != TenantStatus.ACTIVE) {
            throw new BusinessRuleException("Tenant is not active");
        }
        if (apartment.getStatus() == ApartmentStatus.INACTIVE
                || apartment.getStatus() == ApartmentStatus.UNDER_MAINTENANCE) {
            throw new BusinessRuleException("Apartment is not rentable (status " + apartment.getStatus() + ")");
        }
        assertNoOverlap(apartment.getId(), req.startDate(), req.endDate(), null);

        RentalAgreement a = new RentalAgreement();
        a.setAgreementNumber(nextNumber());
        a.setApartment(apartment);
        a.setTenant(tenant);
        a.setStartDate(req.startDate());
        a.setEndDate(req.endDate());
        a.setMonthlyRent(req.monthlyRent());
        a.setDeposit(req.deposit());
        a.setPaymentFrequency(req.paymentFrequency());
        a.setRentDueDay(req.rentDueDay());
        a.setStatus(AgreementStatus.ACTIVE);
        UserPrincipal actor = actor();
        a.setCreatedBy(actor != null ? actor.getId() : null);
        a.setCreatedByName(actor != null ? actor.getUsername() : null);
        if (req.terms() != null) {
            int i = 0;
            for (String content : req.terms()) {
                RentalAgreementTerm t = new RentalAgreementTerm();
                t.setAgreement(a);
                t.setTermOrder(++i);
                t.setContent(content);
                a.getTerms().add(t);
            }
        }
        RentalAgreement saved = agreementRepository.save(a);
        generateSchedules(saved);
        apartment.setStatus(ApartmentStatus.OCCUPIED);
        apartmentRepository.save(apartment);
        record(saved, "CREATED", null, AgreementStatus.ACTIVE, "Agreement created", actor);
        return RentalAgreementResponse.of(saved);
    }

    @Transactional(readOnly = true)
    public RentalAgreementResponse get(Long id) {
        return RentalAgreementResponse.of(find(id));
    }

    @Transactional(readOnly = true)
    public PageResponse<RentalAgreementResponse> search(Long apartmentId, Long tenantId,
                                                        AgreementStatus status, int page, int size) {
        return PageResponse.of(
                agreementRepository.search(apartmentId, tenantId, status,
                        PageRequest.of(page, size, Sort.by("createdAt").descending())),
                RentalAgreementResponse::of);
    }

    @Transactional(readOnly = true)
    public List<RentScheduleResponse> schedules(Long agreementId) {
        find(agreementId);
        return scheduleRepository.findByAgreementIdOrderByPeriodStart(agreementId).stream()
                .map(RentScheduleResponse::of).toList();
    }

    @Transactional(readOnly = true)
    public List<HistoryEntryResponse> history(Long agreementId) {
        find(agreementId);
        return historyRepository.findByAgreementIdOrderByCreatedAtDesc(agreementId).stream()
                .map(h -> new HistoryEntryResponse(h.getEvent(), h.getFromStatus(), h.getToStatus(),
                        h.getNote(), h.getActorUsername(), h.getCreatedAt()))
                .toList();
    }

    @Transactional
    public RentalAgreementResponse renew(Long id, LocalDate newEndDate) {
        RentalAgreement a = find(id);
        if (a.getStatus() != AgreementStatus.ACTIVE) {
            throw new BusinessRuleException("Only active agreements can be renewed");
        }
        if (!newEndDate.isAfter(a.getEndDate())) {
            throw new BusinessRuleException("New end date must extend the agreement");
        }
        assertNoOverlap(a.getApartment().getId(), a.getEndDate().plusDays(1), newEndDate, a.getId());
        LocalDate from = a.getEndDate();
        a.setEndDate(newEndDate);
        extendSchedules(a, from.plusDays(1));
        RentalAgreement saved = agreementRepository.save(a);
        record(saved, "RENEWED", AgreementStatus.ACTIVE, AgreementStatus.ACTIVE,
                "Renewed until " + newEndDate, actor());
        return RentalAgreementResponse.of(saved);
    }

    @Transactional
    public RentalAgreementResponse terminate(Long id, String reason) {
        RentalAgreement a = find(id);
        if (a.getStatus() != AgreementStatus.ACTIVE && a.getStatus() != AgreementStatus.DRAFT) {
            throw new BusinessRuleException("Agreement is already " + a.getStatus());
        }
        AgreementStatus from = a.getStatus();
        a.setStatus(AgreementStatus.TERMINATED);
        a.setTerminationReason(reason);
        cancelFutureSchedules(a);
        RentalAgreement saved = agreementRepository.save(a);
        Apartment apt = a.getApartment();
        if (!agreementRepository.overlapsActive(apt.getId(), LocalDate.now().minusYears(50),
                LocalDate.now().plusYears(50), BLOCKING_STATUSES, null)) {
            apt.setStatus(ApartmentStatus.AVAILABLE);
            apartmentRepository.save(apt);
        }
        record(saved, "TERMINATED", from, AgreementStatus.TERMINATED, reason, actor());
        return RentalAgreementResponse.of(saved);
    }

    private RentalAgreement find(Long id) {
        return agreementRepository.findWithDetailsById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Rental agreement not found: " + id));
    }

    private void assertNoOverlap(Long apartmentId, LocalDate start, LocalDate end, Long excludeId) {
        if (agreementRepository.overlapsActive(apartmentId, start, end, BLOCKING_STATUSES, excludeId)) {
            throw new BusinessRuleException(
                    "Apartment already has an overlapping rental agreement for this period");
        }
    }

    /** Generates one schedule row per payment period covering [startDate, endDate]. */
    private void generateSchedules(RentalAgreement a) {
        scheduleRepository.saveAll(buildSchedules(a, a.getStartDate()));
    }

    private void extendSchedules(RentalAgreement a, LocalDate from) {
        scheduleRepository.saveAll(buildSchedules(a, from));
    }

    private List<RentSchedule> buildSchedules(RentalAgreement a, LocalDate from) {
        int monthsPerPeriod = switch (a.getPaymentFrequency()) {
            case MONTHLY -> 1;
            case QUARTERLY -> 3;
            case ANNUALLY -> 12;
        };
        List<RentSchedule> schedules = new ArrayList<>();
        LocalDate periodStart = from;
        while (!periodStart.isAfter(a.getEndDate())) {
            LocalDate periodEnd = periodStart.plusMonths(monthsPerPeriod).minusDays(1);
            if (periodEnd.isAfter(a.getEndDate())) periodEnd = a.getEndDate();
            long months = ChronoUnit.MONTHS.between(periodStart, periodEnd.plusDays(1));
            if (months <= 0) months = 1;

            RentSchedule s = new RentSchedule();
            s.setAgreement(a);
            s.setPeriodStart(periodStart);
            s.setPeriodEnd(periodEnd);
            s.setDueDate(dueDateFor(periodStart, a.getRentDueDay()));
            s.setAmount(a.getMonthlyRent().multiply(java.math.BigDecimal.valueOf(months)));
            schedules.add(s);
            periodStart = periodEnd.plusDays(1);
        }
        return schedules;
    }

    /** Due day clamped to the length of the period's month. */
    private LocalDate dueDateFor(LocalDate periodStart, int dueDay) {
        int clamped = Math.min(dueDay, periodStart.lengthOfMonth());
        LocalDate due = periodStart.withDayOfMonth(clamped);
        return due.isBefore(periodStart) ? periodStart : due;
    }

    private void cancelFutureSchedules(RentalAgreement a) {
        LocalDate today = LocalDate.now();
        for (RentSchedule s : scheduleRepository.findByAgreementIdOrderByPeriodStart(a.getId())) {
            if (s.getPeriodStart().isAfter(today) && s.getStatus() == ScheduleStatus.PENDING) {
                s.setStatus(ScheduleStatus.CANCELLED);
                scheduleRepository.save(s);
            }
        }
    }

    private void record(RentalAgreement a, String event, AgreementStatus from,
                        AgreementStatus to, String note, UserPrincipal actor) {
        RentalAgreementHistory h = new RentalAgreementHistory();
        h.setAgreement(a);
        h.setEvent(event);
        h.setFromStatus(from != null ? from.name() : null);
        h.setToStatus(to != null ? to.name() : null);
        h.setNote(note);
        h.setActorId(actor != null ? actor.getId() : null);
        h.setActorUsername(actor != null ? actor.getUsername() : null);
        historyRepository.save(h);
    }

    private String nextNumber() {
        String number;
        do {
            number = String.format("AGR-%04d", agreementRepository.maxAgreementSequence() + 1);
        } while (agreementRepository.existsByAgreementNumber(number));
        return number;
    }
}
