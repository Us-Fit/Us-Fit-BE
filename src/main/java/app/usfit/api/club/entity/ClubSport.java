package app.usfit.api.club.entity;

import app.usfit.api.sport.entity.Sport;
import jakarta.persistence.*;
import lombok.*;

/**
 * ClubSport (동호회-종목 매핑) 엔티티
 * - 동호회가 어떤 종목을 다루는지, 레벨 범위/비고 등을 보관
 * - (club_id, sport_id) 유니크 보장
 */
@Entity
@Table(
        name = "club_sport",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_club_sport",
                columnNames = {"club_id", "sport_id"}
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class ClubSport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // Surrogate Key

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "club_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_club_sport_club"))
    private Club club; // 동호회 FK

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sport_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_club_sport_sport"))
    private Sport sport; // 종목 FK

    private Integer levelMin; // 최소 요구 레벨 (선택)
    private Integer levelMax; // 최대 레벨 (선택)

    @Column(length = 200)
    private String note;      // 비고
}
