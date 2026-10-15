package app.usfit.api.RecruitPlayer.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import app.usfit.api.RecruitPlayer.entity.RecruitApplication;

public interface RecruitApplicationRepository extends JpaRepository<RecruitApplication, Long> {
    boolean existsByPost_IdAndApplicant_Id(Long postId, Long applicantId);
    List<RecruitApplication> findByPost_IdOrderByApplicationIdDesc(Long postId);
    List<RecruitApplication> findByApplicant_IdOrderByApplicationIdDesc(Long userId);

    // 상태 전이: 현재 상태가 from일 때만 to로 변경 (반환값 0이면 다른 요청이 먼저 상태를 바꾼 것)
    @Modifying
    @Query("UPDATE RecruitApplication a SET a.status = :to WHERE a.applicationId = :applicationId AND a.status = :from")
    int transitionStatus(@Param("applicationId") Long applicationId,
                         @Param("from") RecruitApplication.Status from,
                         @Param("to") RecruitApplication.Status to);
}
