package app.usfit.api.facility.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * FacilityAddress (시설 주소/행정구역/좌표) 엔티티
 * - 도로명/지번/행정코드/좌표를 보관
 */
@Entity
@Table(name = "facility_address")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class FacilityAddress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // 주소 PK

    @ManyToOne(fetch = FetchType.LAZY) // N:1 (여러 주소가 하나 시설에 속함)
    @JoinColumn(name = "facility_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_facility_address_facility"))
    private Facility facility; // 시설 FK

    // 도로명/지번 주소 및 우편번호 (원천 문자열 그대로 보존)
    @Column(length = 6)
    private String roadZip;   // 도로명 우편번호 (ROAD_NM_ZIP_NO)

    @Column(length = 500)
    private String roadAddr1; // 도로명 주소1 (RDNMADR_ONE_NM)

    @Column(length = 500)
    private String roadAddr2; // 도로명 주소2 (RDNMADR_TWO_NM)

    @Column(length = 6)
    private String zipValue;  // 우편번호 값 (ZIP_NO_VALUE)

    @Column(length = 500)
    private String addr1;     // 지번 주소1 (FCLTY_ADDR_ONE_NM)

    @Column(length = 500)
    private String addr2;     // 지번 주소2 (FCLTY_ADDR_TWO_NM)

    // 행정구역 코드/명 (표준코드/이름 함께 저장)
    @Column(length = 20)
    private String sidoCd;    // 시도 코드 (CTPRVN_CD)

    @Column(length = 50)
    private String sidoNm;    // 시도 명 (CTPRVN_NM)

    @Column(length = 20)
    private String sigunguCd; // 시군구 코드 (SIGNGU_CD)

    @Column(length = 50)
    private String sigunguNm; // 시군구 명 (SIGNGU_NM)

    // 관리 관할(행정)
    @Column(length = 10)
    private String mngSidoCd; // 관리 시도 코드

    @Column(length = 50)
    private String mngSidoNm; // 관리 시도 명

    @Column(length = 10)
    private String mngSigunguCd; // 관리 시군구 코드

    @Column(length = 50)
    private String mngSigunguNm; // 관리 시군구 명

    @Column(length = 20)
    private String mngEmdCd;  // 관리 읍면동 코드

    @Column(length = 50)
    private String mngEmdNm;  // 관리 읍면동 명

    @Column(length = 20)
    private String mngLiCd;   // 관리 리 코드

    @Column(length = 20)
    private String mngLiNm;   // 관리 리 명

    // 좌표 (decimal(9,6) → BigDecimal 권장)
    @Column(precision = 9, scale = 6)
    private BigDecimal lat;   // 위도 (FCLTY_LA)

    @Column(precision = 9, scale = 6)
    private BigDecimal lng;   // 경도 (FCLTY_LO)
}
