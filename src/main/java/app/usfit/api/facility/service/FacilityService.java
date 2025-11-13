package app.usfit.api.facility.service;

import app.usfit.api.facility.dto.FacilityDetailView;
import app.usfit.api.facility.repository.FacilityRepository;
import app.usfit.api.sport.repository.SportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FacilityService {

    private final FacilityRepository facilityRepository;

    private final SportRepository sportRepository;

    public List<FacilityDetailView> findNearbyFacilitiesBySportNames(
            double lat, double lng, double radiusKm, List<String> sportNames, int limit, int offset
    ) {
        boolean noFilter = (sportNames == null || sportNames.isEmpty());

        List<Long> sportIds;
        if (noFilter) {
            sportIds = List.of();
        } else {
            // 이름 정규화
            Set<String> normalized = sportNames.stream()
                    .filter(Objects::nonNull)
                    .map(s -> s.trim().toLowerCase())
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toCollection(LinkedHashSet::new));

            if (normalized.isEmpty()) {
                return List.of(); // 전부 공백/널이면 빈 결과
            }

            sportIds = sportRepository.findIdsByNormalizedNames(normalized);

            // 요청에 이름은 있었는데 매칭된 id가 하나도 없으면 바로 빈 결과 반환
            if (sportIds.isEmpty()) {
                return List.of();
            }
        }

        return facilityRepository.findNearbyFacilitiesNative(
                lat, lng, radiusKm, sportIds, sportIds.isEmpty(), limit, offset
        );
    }
}