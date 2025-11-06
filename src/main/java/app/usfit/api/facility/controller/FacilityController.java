package app.usfit.api.facility.controller;

import app.usfit.api.facility.dto.FacilityDetailDto;
import app.usfit.api.facility.repository.FacilityRepository;
import app.usfit.api.facility.repository.FacilitySportRepository;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RestController
@RequestMapping("/api/facility")
@RequiredArgsConstructor
@Tag(name = "🏟 Facility API", description = "운동시설 상세조회 및 조건검색 기능을 제공합니다.")
public class FacilityController {

    private final FacilityRepository facilityRepository;
    private final FacilitySportRepository facilitySportRepository;

    // -------------------- [1] 시설 상세 조회 --------------------
    @Operation(
            summary = "시설 상세 정보 조회",
            description = """
                    시설 ID(facility id)를 이용해 해당 시설의 상세 정보를 조회합니다.<br>
                    - 시설명, 유형, 행정구역명, 주소, 좌표(lat/lng), 관리자 연락처, 면적, 실내/실외 여부 등의 정보를 반환합니다.<br>
                    ✅ 예시 요청: `/api/facility/1`
                    """
    )
    @GetMapping("/{id}")
    public FacilityDetailDto getFacilityDetail(
            @Parameter(description = "시설 ID (예: 1)", example = "1", required = true)
            @PathVariable Long id
    ) {
        return facilityRepository.getFacilityInfo(id);
    }

    // -------------------- [2] 스포츠명 + 시군구 검색 --------------------
    @Operation(
            summary = "스포츠 및 지역명으로 시설 검색",
            description = """
                    특정 스포츠 종목명과 시군구명을 기반으로 시설 목록을 검색합니다.<br>
                    - 예: '간이운동장' + '김천시' → 김천시 내 간이운동장 시설 목록 반환<br>
                    - 반환 항목: 시설명, 유형, 주소, 좌표, 면적, 실내/실외, 관리자 연락처 등
                    """
    )
    @GetMapping("/search")
    public List<FacilityDetailDto> getFacilitiesBySportAndSigungu(
            @Parameter(description = "스포츠 이름 (예: 간이운동장)", example = "간이운동장", required = true)
            @RequestParam String sportName,

            @Parameter(description = "시군구 이름 (예: 김천시)", example = "김천시", required = true)
            @RequestParam String sigunguNm
    ) {
        return facilitySportRepository.findFacilitiesBySportNameAndSigungu(sportName, sigunguNm);
    }
}