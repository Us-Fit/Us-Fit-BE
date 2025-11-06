package app.usfit.api.user.entity;

import app.usfit.api.common.enums.InterestLevel;
import app.usfit.api.sport.entity.Sport;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "user_interest_sport")
@Data
public class UserInterestSport {
    // EmbeddedId: 복합키 객체 삽입
    // ManyToOne: 다대일 관계 매핑
    // MapsId: 복합키의 일부 필드를 매핑
    // JoinColumn: 외래키 컬럼 지정 

    @EmbeddedId
    private UserInterestSportId id; 

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("userId")
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("sportId")
    @JoinColumn(name = "sport_id")
    private Sport sport;

    @Enumerated(EnumType.STRING)
    @Column(name = "skill_level")
    private InterestLevel skillLevel;

    protected UserInterestSport() {}

    public UserInterestSport(User user, Sport sport, InterestLevel skillLevel) {
        this.user = user;
        this.sport = sport;
        this.skillLevel = skillLevel;
        this.id = new UserInterestSportId(user.getId(), sport.getId());
    }
}
