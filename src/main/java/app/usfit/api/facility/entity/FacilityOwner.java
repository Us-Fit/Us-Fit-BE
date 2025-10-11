package app.usfit.api.facility.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * FacilityOwner (소유주체) 엔티티
 * - 시설의 소유 주체 코드/명 및 지역 정보를 보관
 */
@Entity
@Table(name = "facility_owner")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PUBLIC)
@AllArgsConstructor
@Builder
public class FacilityOwner {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // 소유 레코드 PK

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "facility_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_facility_owner_facility"))
    private Facility facility; // 시설 FK

    @Column(length = 20)
    private String ownerCd;       // 소유 주체 코드 (POSESN_MBY_CD)

    @Column(length = 200)
    private String ownerNm;       // 소유 주체 명 (POSESN_MBY_NM)

    @Column(length = 20)
    private String ownerSidoCd;   // 소유 주체 시도 코드

    @Column(length = 50)
    private String ownerSidoNm;   // 소유 주체 시도 명

    @Column(length = 20)
    private String ownerSigunguCd; // 소유 주체 시군구 코드

    @Column(length = 50)
    private String ownerSigunguNm; // 소유 주체 시군구 명
}
