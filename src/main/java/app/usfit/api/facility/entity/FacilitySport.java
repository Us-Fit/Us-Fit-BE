package app.usfit.api.facility.entity;

import app.usfit.api.sport.entity.Sport;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * FacilitySport (시설-종목 매핑) 엔티티
 * - 한 시설이 어떤 종목을 운영하는지 + 종목별 운영 특성(요금/코트/실내여부) 보관
 * - (facility_id, sport_id) 조합 유니크 보장
 */
@Entity
@Table(
        name = "facility_sport",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_facility_sport",
                columnNames = {"facility_id", "sport_id"}
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class FacilitySport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // Surrogate Key (복합 PK 대신 단일 PK 사용)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "facility_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_facility_sport_facility"))
    private Facility facility; // 시설 FK

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sport_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_facility_sport_sport"))
    private Sport sport; // 종목 FK

    @Column(precision = 10, scale = 2)
    private BigDecimal pricePerHour; // 시간당 요금 (선택)

    private Integer courtCount;      // 코트/구장 수 (선택)

    private Boolean indoor;          // 실내 여부 (선택)

    @Column(length = 200)
    private String note;             // 비고 (운영 메모 등)
}
