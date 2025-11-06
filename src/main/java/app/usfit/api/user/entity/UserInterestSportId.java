package app.usfit.api.user.entity;

import java.io.Serializable;
import java.util.Objects;

import jakarta.persistence.Embeddable;
import lombok.Data;

@Embeddable
@Data
public class UserInterestSportId implements Serializable {
    private Long userId;
    private Long sportId;

    public UserInterestSportId() {}
    public UserInterestSportId(Long userId, Long sportId) {
        this.userId = userId;
        this.sportId = sportId;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof UserInterestSportId that)) return false;
        return Objects.equals(userId, that.userId) && Objects.equals(sportId, that.sportId);
    }

    @Override public int hashCode() { return Objects.hash(userId, sportId); }
}
