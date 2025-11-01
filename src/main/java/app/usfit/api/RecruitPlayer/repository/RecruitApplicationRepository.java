package app.usfit.api.RecruitPlayer.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import app.usfit.api.RecruitPlayer.entity.RecruitApplication;

public interface RecruitApplicationRepository extends JpaRepository<RecruitApplication, Long> {
    boolean existsByPost_IdAndApplicant_Id(Long postId, Long applicantId);
    List<RecruitApplication> findByPost_IdOrderByApplicationIdDesc(Long postId);
    List<RecruitApplication> findByApplicant_IdOrderByApplicationIdDesc(Long userId);
}
