package app.usfit.api.facility.dto;

import java.math.BigDecimal;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(description = "시설 상세 정보 DTO")
public class FacilityDetailDto {

    @Schema(description = "시설 이름", example = "김천종합운동장")
    private String name;

    @Schema(description = "시설 유형", example = "운동장")
    private String typeName;

    @Schema(description = "시/도 명", example = "경상북도")
    private String sidoNm;

    @Schema(description = "시/군/구 명", example = "김천시")
    private String sigunguNm;

    @Schema(description = "도로명 주소", example = "김천시 시청로 1")
    private String roadAddr1;

    @Schema(description = "위도", example = "36.133")
    private BigDecimal lat;

    @Schema(description = "경도", example = "128.113")
    private BigDecimal lng;

    @Schema(description = "담당자 연락처", example = "054-420-1234")
    private String managerPhone;

    @Schema(description = "면적(m²)", example = "4200")
    private Integer areaSqm;

    @Schema(description = "실내/실외 구분", example = "실외")
    private String indoorOutdoor;

    //순서/타입 전부 JPQL과 같아야 함.
    public FacilityDetailDto(
            String name,
            String typeName,
            String sidoNm,
            String sigunguNm,
            String roadAddr1,
            BigDecimal lat,
            BigDecimal lng,
            String managerPhone,
            Integer areaSqm,
            String indoorOutdoor
    ) {
        this.name = name;
        this.typeName = typeName;
        this.sidoNm = sidoNm;
        this.sigunguNm = sigunguNm;
        this.roadAddr1 = roadAddr1;
        this.lat = lat;
        this.lng = lng;
        this.managerPhone = managerPhone;
        this.areaSqm = areaSqm;
        this.indoorOutdoor = indoorOutdoor;
    }

    @Override
    public String toString() {
        return "FacilityDetailDto{" +
                "name='" + name + '\'' +
                ", typeName='" + typeName + '\'' +
                ", sidoNm='" + sidoNm + '\'' +
                ", sigunguNm='" + sigunguNm + '\'' +
                ", roadAddr1='" + roadAddr1 + '\'' +
                ", lat=" + lat +
                ", lng=" + lng +
                ", managerPhone='" + managerPhone + '\'' +
                ", areaSqm=" + areaSqm +
                ", indoorOutdoor='" + indoorOutdoor + '\'' +
                '}';
    }
}
