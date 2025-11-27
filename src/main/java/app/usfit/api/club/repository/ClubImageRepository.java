package app.usfit.api.club.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import app.usfit.api.club.entity.ClubImage;

public interface ClubImageRepository extends JpaRepository<ClubImage, Long> {
    List<ClubImage> findByClubIdOrderByCreatedAtDesc(Long clubId);
    List<ClubImage> findByUploaderId(Long uploaderId);
    void deleteByClubId(Long clubId);
}