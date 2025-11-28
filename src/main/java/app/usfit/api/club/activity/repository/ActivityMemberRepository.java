package app.usfit.api.club.activity.repository;

import app.usfit.api.club.activity.entity.ActivityMember;
import app.usfit.api.club.activity.entity.ActivityMemberStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ActivityMemberRepository extends JpaRepository<ActivityMember, Long> {

    boolean existsByActivityIdAndUserIdAndStatus(Long activityId, Long userId, ActivityMemberStatus status);

    List<ActivityMember> findByActivityIdAndStatus(Long activityId, ActivityMemberStatus status);

    Optional<ActivityMember> findByActivityIdAndUserId(Long activityId, Long userId);
}
