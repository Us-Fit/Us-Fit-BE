package app.usfit.api.facility.controller;

import app.usfit.api.facility.dto.FacilityDetailDto;
import app.usfit.api.facility.dto.FacilityDetailView;
import app.usfit.api.facility.repository.FacilityRepository;
import app.usfit.api.facility.repository.FacilitySportRepository;
import app.usfit.api.facility.service.FacilityService;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.constraints.*;
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
    private final FacilityService facilityService;

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

            @Parameter(description = "시 이름 (예: 경상북도)", example = "경상북도", required = true)
            @RequestParam String sidoNm,

            @Parameter(description = "시군구 이름 (예: 김천시)", example = "김천시", required = true)
            @RequestParam String sigunguNm
    ) {
        return facilitySportRepository.findFacilitiesBySportNameAndSigungu(sportName, sidoNm, sigunguNm);
    }

    /**
     * 예시:
     * GET /api/facility/nearby?lat=37.5665&lng=126.9780&radiusKm=2&limit=50&offset=0&sportNames=농구,체력단련장
     * - sportNames 생략 가능(빈 목록이면 전체 종목)
     * - radiusKm 단위: km
     */
    @Operation(
            summary = "반경 내 시설 검색",
            description = "요청 좌표(lat,lng)를 중심으로 radiusKm(km) 반경 내 시설을 거리순으로 반환합니다.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "성공",
                            content = @Content(
                                    mediaType = "application/json",
                                    array = @ArraySchema(schema = @Schema(implementation = FacilityDetailView.class)),
                                    examples = @ExampleObject(
                                            name = "sample",
                                            value = """
                    [
                      {
                        "name": "헬스보이짐 앤 필라걸 서울시청점",
                        "typeName": "체력단련장",
                        "id": 98405,
                        "distance": 164.14828844066957,
                        "roadAddr1": "서울특별시 중구 무교로 21(무교동)",
                        "lat": 37.567854,
                        "lng": 126.978742,
                        "indoorOutdoor": "없음",
                        "sigunguNm": "중구",
                        "managerPhone": null,
                        "areaSqm": 1228,
                        "sidoNm": "서울특별시"
                      },
                      {
                        "name": "피트니스비엠 ㈜피트비엠",
                        "typeName": "체력단련장",
                        "id": 93588,
                        "distance": 212.57375692138376,
                        "roadAddr1": "서울특별시 중구 세종대로 135(태평로1가)",
                        "lat": 37.567813,
                        "lng": 126.976247,
                        "indoorOutdoor": "실내",
                        "sigunguNm": "중구",
                        "managerPhone": null,
                        "areaSqm": 0,
                        "sidoNm": "서울특별시"
                      }
                    ]
                    """
                                    )
                            )
                    ),
                    @ApiResponse(responseCode = "400", description = "잘못된 요청 파라미터", content = @Content),
                    @ApiResponse(responseCode = "500", description = "서버 오류", content = @Content)
            }
    )
    @GetMapping("/nearby")
    public List<FacilityDetailView> getNearby(
            @Parameter(
                    in = ParameterIn.QUERY,
                    description = "위도(-90~90)",
                    example = "37.5665"
            )
            @RequestParam @DecimalMin(value = "-90") @DecimalMax(value = "90") double lat,

            @Parameter(
                    in = ParameterIn.QUERY,
                    description = "경도(-180~180)",
                    example = "126.9780"
            )
            @RequestParam @DecimalMin(value = "-180") @DecimalMax(value = "180") double lng,

            @Parameter(
                    in = ParameterIn.QUERY,
                    description = "반경(KM). 기본 2km",
                    example = "2"
            )
            @RequestParam(defaultValue = "2") @Positive double radiusKm,

            @Parameter(
                    in = ParameterIn.QUERY,
                    description = """
                    종목 이름 목록. 다음 두 방식 모두 지원:
                    1) CSV: sportNames=농구,체력단련장
                    2) 반복파라미터: sportNames=농구&sportNames=체력단련장
                    대소문자/공백은 서비스단에서 정규화함.
                    """,
                    array = @ArraySchema(schema = @Schema(type = "string")),
                    examples = {
                            @ExampleObject(name = "CSV", value = "농구,체력단련장"),
                            @ExampleObject(name = "Repeated", value = "농구&sportNames=체력단련장")
                    }
            )
            @RequestParam(name = "sportNames", required = false) List<String> sportNames,

            @Parameter(in = ParameterIn.QUERY, description = "최대 개수(기본 50)", example = "50")
            @RequestParam(defaultValue = "50") @Positive int limit,

            @Parameter(in = ParameterIn.QUERY, description = "오프셋(기본 0)", example = "0")
            @RequestParam(defaultValue = "0") @PositiveOrZero int offset
    ) {
        return facilityService.findNearbyFacilitiesBySportNames(lat, lng, radiusKm, sportNames, limit, offset);
    }

}