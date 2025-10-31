package app.usfit.api.RecruitPlayer.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import app.usfit.api.RecruitPlayer.entity.RecruitPlayerPost;

@Repository
public interface RecruitPlayerPostRepository extends JpaRepository<RecruitPlayerPost, Long> {
    List<RecruitPlayerPost> findByIsActiveTrue();
    java.util.List<RecruitPlayerPost> findByWriter_IdAndIsActiveTrueOrderByIdDesc(Long writerId);
    java.util.List<RecruitPlayerPost> findByWriter_IdOrderByIdDesc(Long writerId);
}
