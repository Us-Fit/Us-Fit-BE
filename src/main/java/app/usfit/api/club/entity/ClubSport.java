package app.usfit.api.club.entity;

import app.usfit.api.sport.entity.Sport;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

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

    @Column(name = "level_min")
    private Integer levelMin; // 최소 요구 레벨 (선택)

    @Column(name = "level_max")
    private Integer levelMax; // 최대 레벨 (선택)

    @Column(length = 200)
    private String note;      // 노트

    public void setClub(Club club) {
        this.club = club;
        if (club.getSports() == null) {
                club.setSports(new java.util.ArrayList<>());
            }

        if (club != null && !club.getSports().contains(this)) {
            club.getSports().add(this);
        }
    }
}
