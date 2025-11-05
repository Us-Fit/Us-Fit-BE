package app.usfit.api.sport.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import app.usfit.api.sport.entity.Sport;
import app.usfit.api.sport.entity.UserSport;
import app.usfit.api.user.entity.User;

public interface UserSportRepository extends JpaRepository<UserSport, Long> {

    // 한 유저의 모든 종목
    List<UserSport> findByUser(User user);

    // 특정 종목을 가진 모든 유저
    List<UserSport> findBySport(Sport sport);

    // 유저-종목 한 행
    Optional<UserSport> findByUserAndSport(User user, Sport sport);

    // 중복 체크 (unique 제약과 함께 사용)
    boolean existsByUserAndSport(User user, Sport sport);
}
