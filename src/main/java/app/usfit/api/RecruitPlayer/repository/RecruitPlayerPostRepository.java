package app.usfit.api.RecruitPlayer.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import app.usfit.api.RecruitPlayer.entity.RecruitPlayerPost;

@Repository
public interface RecruitPlayerPostRepository extends JpaRepository<RecruitPlayerPost, Long> {
    List<RecruitPlayerPost> findByIsActiveTrue();
    java.util.List<RecruitPlayerPost> findByWriter_IdAndIsActiveTrueOrderByIdDesc(Long writerId);
    java.util.List<RecruitPlayerPost> findByWriter_IdOrderByIdDesc(Long writerId);

    // 자리 확보: 정원 미만일 때만 +1 (반환값 0이면 정원 마감)
    @Modifying
    @Query("UPDATE RecruitPlayerPost p SET p.acceptedCount = p.acceptedCount + 1 WHERE p.id = :postId AND p.acceptedCount < p.maxMember")
    int reserveSeat(@Param("postId") Long postId);

    // 자리 반납: 0 미만으로 내려가지 않도록 -1
    @Modifying
    @Query("UPDATE RecruitPlayerPost p SET p.acceptedCount = p.acceptedCount - 1 WHERE p.id = :postId AND p.acceptedCount > 0")
    int releaseSeat(@Param("postId") Long postId);
}
