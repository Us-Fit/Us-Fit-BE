package app.usfit.api.user.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import app.usfit.api.user.entity.UserInterestSport;
import app.usfit.api.user.entity.UserInterestSportId;

public interface UserInterestSportRepository extends JpaRepository<UserInterestSport, UserInterestSportId>{
    @Transactional
    void deleteByUser_Id(Long userId);

    // 조회
    List<UserInterestSport> findByUser_Id(Long userId);

    // UserInterestSport.user.id 기준으로 조회
    List<UserInterestSport> findByUser_IdIn(List<Long> userIds);
}
