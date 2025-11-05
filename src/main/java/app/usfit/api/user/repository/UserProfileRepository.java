package app.usfit.api.user.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import app.usfit.api.user.entity.UserProfile;

public interface UserProfileRepository extends JpaRepository<UserProfile, Object> {
    Optional<UserProfile> findByUserId(Long userId);     // PK=FK로 조회
    Optional<UserProfile> findByNickname(String nickname); // 닉네임은 UserProfile 컬럼
    void deleteByUser_Id(Long userId);
} 
