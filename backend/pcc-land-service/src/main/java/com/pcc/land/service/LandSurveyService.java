package com.pcc.land.service;

import com.pcc.land.dto.request.SurveyRequest;
import com.pcc.land.dto.response.PageResponse;
import com.pcc.land.dto.response.SurveyResponse;
import com.pcc.land.entity.LandParcel;
import com.pcc.land.entity.LandSurvey;
import com.pcc.land.enums.TransactionType;
import com.pcc.land.exception.ResourceNotFoundException;
import com.pcc.land.repository.LandSurveyRepository;
import com.pcc.land.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LandSurveyService {

    private final LandSurveyRepository surveyRepository;
    private final LandSupport support;

    @Transactional
    public SurveyResponse create(SurveyRequest req) {
        LandParcel parcel = support.parcel(req.parcelId());
        UserPrincipal actor = support.actor();
        LandSurvey s = new LandSurvey();
        s.setParcel(parcel);
        s.setSurveyReference(nextRef());
        apply(s, req);
        if (actor != null) {
            s.setCreatedBy(actor.getId());
            s.setCreatedByName(actor.getUsername());
        }
        s = surveyRepository.save(s);
        support.tx(parcel, TransactionType.SURVEY_RECORDED, s.getId(), s.getSurveyReference(),
                null, null, "Survey recorded: " + s.getSurveyReference());
        return SurveyResponse.from(s);
    }

    public PageResponse<SurveyResponse> list(Long parcelId, String search, int page, int size) {
        var pageable = PageRequest.of(page, Math.min(size, 100), Sort.by("surveyDate").descending());
        return PageResponse.of(surveyRepository.search(parcelId, LandParcelService.blank(search), pageable),
                SurveyResponse::from);
    }

    public java.util.List<SurveyResponse> byParcel(Long parcelId) {
        support.parcel(parcelId);
        return surveyRepository.findByParcelIdOrderBySurveyDateDesc(parcelId)
                .stream().map(SurveyResponse::from).toList();
    }

    @Transactional
    public SurveyResponse update(Long id, SurveyRequest req) {
        LandSurvey s = surveyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Survey not found"));
        apply(s, req);
        return SurveyResponse.from(surveyRepository.save(s));
    }

    private void apply(LandSurvey s, SurveyRequest req) {
        s.setSurveyDate(req.surveyDate());
        s.setSurveyorName(req.surveyorName());
        s.setSurveyorOrganization(req.surveyorOrganization());
        s.setSurveyedArea(req.surveyedArea());
        s.setBoundaryNorth(req.boundaryNorth());
        s.setBoundarySouth(req.boundarySouth());
        s.setBoundaryEast(req.boundaryEast());
        s.setBoundaryWest(req.boundaryWest());
        s.setDescription(req.description());
        s.setDocumentReference(req.documentReference());
    }

    private String nextRef() {
        String ref;
        do {
            ref = LandSupport.ref("SRV-", surveyRepository.maxSurveySequence() + 1);
        } while (surveyRepository.existsBySurveyReference(ref));
        return ref;
    }
}
