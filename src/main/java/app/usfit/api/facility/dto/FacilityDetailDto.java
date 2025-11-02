package app.usfit.api.facility.dto;

import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor // JPQL projection은 없어도 되지만 다른 곳에서 쓸 수 있으니 놔도 됨
public class FacilityDetailDto {

    private String name;
    private String typeName;
    private String sidoNm;
    private String sigunguNm;
    private String roadAddr1;
    private BigDecimal lat;
    private BigDecimal lng;

    // 💡 이 생성자가 핵심이야. 순서/타입 전부 JPQL과 같아야 함.
    public FacilityDetailDto(
            String name,
            String typeName,
            String sidoNm,
            String sigunguNm,
            String roadAddr1,
            BigDecimal lat,
            BigDecimal lng
    ) {
        this.name = name;
        this.typeName = typeName;
        this.sidoNm = sidoNm;
        this.sigunguNm = sigunguNm;
        this.roadAddr1 = roadAddr1;
        this.lat = lat;
        this.lng = lng;
    }
}
