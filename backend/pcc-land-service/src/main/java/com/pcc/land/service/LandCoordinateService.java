package com.pcc.land.service;

import com.pcc.land.dto.request.CoordinatesRequest;
import com.pcc.land.dto.response.CoordinateResponse;
import com.pcc.land.entity.LandCoordinate;
import com.pcc.land.entity.LandParcel;
import com.pcc.land.enums.TransactionType;
import com.pcc.land.exception.BusinessRuleException;
import com.pcc.land.repository.LandCoordinateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LandCoordinateService {

    private final LandCoordinateRepository coordinateRepository;
    private final LandSupport support;

    public List<CoordinateResponse> byParcel(Long parcelId) {
        support.parcel(parcelId);
        return coordinateRepository.findByParcelIdOrderBySequence(parcelId)
                .stream().map(CoordinateResponse::from).toList();
    }

    @Transactional
    public List<CoordinateResponse> replace(CoordinatesRequest req) {
        LandParcel parcel = support.parcel(req.parcelId());
        Set<Integer> seen = new HashSet<>();
        for (var point : req.points()) {
            if (!seen.add(point.sequence())) {
                throw new BusinessRuleException("Duplicate coordinate sequence: " + point.sequence());
            }
        }
        coordinateRepository.deleteByParcelId(parcel.getId());
        List<LandCoordinate> saved = req.points().stream().map(p -> {
            LandCoordinate c = new LandCoordinate();
            c.setParcel(parcel);
            c.setSequence(p.sequence());
            c.setLatitude(p.latitude());
            c.setLongitude(p.longitude());
            c.setLabel(p.label());
            return coordinateRepository.save(c);
        }).toList();
        support.tx(parcel, TransactionType.COORDINATES_UPDATED, parcel.getId(),
                parcel.getParcelReference(), null, null,
                saved.size() + " boundary point(s) recorded");
        return saved.stream().map(CoordinateResponse::from).toList();
    }
}
