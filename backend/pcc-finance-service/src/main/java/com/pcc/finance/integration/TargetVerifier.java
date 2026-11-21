package com.pcc.finance.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.pcc.finance.exception.BusinessRuleException;
import com.pcc.finance.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;

/**
 * Fetches and validates external payment targets from their owning services.
 * Finance never writes to Property/Land DBs; it verifies through their APIs and
 * stores a stable snapshot alongside the allocation.
 */
@Component
@RequiredArgsConstructor
public class TargetVerifier {

    private static final Set<String> SALE_PAYABLE_STATUSES =
            Set.of("APPROVED", "PAYMENT_PENDING", "LEGAL_PROCESSING", "COMPLETED");
    private static final Set<String> ACQ_PAYABLE_STATUSES = Set.of("APPROVED", "COMPLETED");

    private final ServiceGateway gateway;

    /** Verify a rent schedule inside a rental agreement (Property Service). */
    public VerifiedTarget rentSchedule(Long agreementId, Long scheduleId) {
        JsonNode schedules = gateway.propertyGet("/api/v1/rental-agreements/" + agreementId + "/schedules")
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Rental agreement not found in Property Service"));
        JsonNode schedule = null;
        if (schedules.isArray()) {
            for (JsonNode s : schedules) {
                if (s.path("id").asLong() == scheduleId) schedule = s;
            }
        }
        if (schedule == null) {
            throw new ResourceNotFoundException("Rent schedule not found in the rental agreement");
        }
        String status = schedule.path("status").asText("");
        if ("CANCELLED".equals(status) || "PAID".equals(status)) {
            throw new BusinessRuleException("Rent schedule is " + status.toLowerCase() + " and cannot be paid");
        }
        JsonNode agreement = gateway.propertyGet("/api/v1/rental-agreements/" + agreementId).orElse(null);
        String agreementNo = agreement != null ? agreement.path("agreementNumber").asText(null) : null;
        String tenant = agreement != null ? agreement.path("tenantName").asText(null) : null;
        String propertyName = agreement != null ? agreement.path("propertyName").asText(null) : null;
        String desc = "Rent " + schedule.path("periodStart").asText("") + " to "
                + schedule.path("periodEnd").asText("")
                + (propertyName != null ? " — " + propertyName : "");
        return new VerifiedTarget(scheduleId, "SCHEDULE-" + scheduleId, agreementNo, desc,
                tenant, dec(schedule, "amount"), schedule.toString());
    }

    /** Verify a land acquisition (Land Service). */
    public VerifiedTarget landAcquisition(Long acquisitionId) {
        JsonNode acq = gateway.landGet("/api/v1/land-acquisitions/" + acquisitionId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Land acquisition not found in Land Service"));
        String status = acq.path("status").asText("");
        if (!ACQ_PAYABLE_STATUSES.contains(status)) {
            throw new BusinessRuleException(
                    "Acquisition is " + status.toLowerCase() + "; only approved acquisitions can be paid");
        }
        String ref = acq.path("acquisitionReference").asText("ACQ-" + acquisitionId);
        String parcel = acq.path("parcelReference").asText(null);
        String seller = acq.path("sellerName").asText(null);
        String desc = "Land acquisition " + ref
                + (acq.path("parcelName").asText(null) != null ? " — " + acq.path("parcelName").asText() : "");
        return new VerifiedTarget(acquisitionId, ref, parcel, desc, seller,
                dec(acq, "purchasePrice"), acq.toString());
    }

    /** Verify a land sale (Land Service). */
    public VerifiedTarget landSale(Long saleId) {
        JsonNode sale = gateway.landGet("/api/v1/land-sales/" + saleId)
                .orElseThrow(() -> new ResourceNotFoundException("Land sale not found in Land Service"));
        String status = sale.path("status").asText("");
        if (!SALE_PAYABLE_STATUSES.contains(status)) {
            throw new BusinessRuleException(
                    "Sale is " + status.toLowerCase() + "; only approved sales can receive payment");
        }
        String ref = sale.path("saleReference").asText("SAL-" + saleId);
        String parcel = sale.path("parcelReference").asText(null);
        String buyer = sale.path("buyerName").asText(null);
        BigDecimal total = sale.path("agreedPrice").isNumber() && sale.path("agreedPrice").decimalValue().signum() > 0
                ? sale.path("agreedPrice").decimalValue()
                : dec(sale, "proposedPrice");
        String desc = "Land sale " + ref
                + (sale.path("parcelName").asText(null) != null ? " — " + sale.path("parcelName").asText() : "");
        return new VerifiedTarget(saleId, ref, parcel, desc, buyer, total, sale.toString());
    }

    /** Sync paid totals back to the owning service (best effort). */
    public void syncRentSchedule(Long scheduleId, BigDecimal paidAmount) {
        gateway.propertyPatchQuietly("/api/v1/rent-schedules/" + scheduleId + "/payment",
                Map.of("paidAmount", paidAmount));
    }

    public void syncLandAcquisition(Long acquisitionId, BigDecimal paidAmount) {
        gateway.landPatchQuietly("/api/v1/land-acquisitions/" + acquisitionId + "/finance-status",
                Map.of("paidAmount", paidAmount));
    }

    public void syncLandSale(Long saleId, BigDecimal paidAmount) {
        gateway.landPatchQuietly("/api/v1/land-sales/" + saleId + "/finance-status",
                Map.of("paidAmount", paidAmount));
    }

    private static BigDecimal dec(JsonNode node, String field) {
        JsonNode v = node.path(field);
        return v.isNumber() ? v.decimalValue() : BigDecimal.ZERO;
    }
}
