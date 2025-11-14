package app.usfit.api.sport.service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import app.usfit.api.sport.entity.Sport;
import app.usfit.api.sport.repository.SportRepository;

@Service
public class SportService {
    private final SportRepository sportRepository;

    public SportService(SportRepository sportRepository) {
        this.sportRepository = sportRepository;
    }

    public Map<String, Sport> findByNamesAsMap(List<String> rawNames) {
        if (rawNames == null || rawNames.isEmpty()) return Map.of();

        // 정규화된 distinct 이름 목록 생성
        List<String> normalized = rawNames.stream()
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(String::toLowerCase)
                .distinct()
                .collect(Collectors.toList());

        if (normalized.isEmpty()) return Map.of();

        // 한 번의 쿼리로 일괄 조회
        List<Sport> existing = sportRepository.findAllByNormalizedNames(normalized);

        // 정규화 이름 -> Sport 맵 반환
        return existing.stream()
                .collect(Collectors.toMap(s -> s.getName().trim().toLowerCase(), s -> s));
    }
    
}
