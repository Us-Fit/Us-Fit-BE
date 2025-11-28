package app.usfit.api.club.activity.repository;

import app.usfit.api.club.activity.entity.Activity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ActivityRepository extends JpaRepository<Activity, Long> {

    List<Activity> findByClubId(Long clubId);
}
