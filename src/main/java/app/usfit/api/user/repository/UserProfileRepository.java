package app.usfit.api.user.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import app.usfit.api.user.entity.UserProfile;
import jakarta.transaction.Transactional;

public interface UserProfileRepository extends JpaRepository<UserProfile, Long> {
    Optional<UserProfile> findByUserId(Long userId);     // PK=FK로 조회
    Optional<UserProfile> findByNickname(String nickname); // 닉네임은 UserProfile 컬럼
    void deleteByUser_Id(Long userId);

    List<UserProfile> findByUserIdIn(List<Long> userIds);

    @Modifying
    @Transactional
    @Query("update UserProfile up set up.profileImageKey = :profileImageKey where up.userId = :userId")
    void setProfileImageKey(@Param("profileImageKey") String profileImageKey, @Param("userId") Long userId);
} 
