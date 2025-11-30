package app.usfit.api.club.recruit.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import app.usfit.api.club.recruit.entity.ClubRecruit;

public interface ClubRecruitRepository extends JpaRepository<ClubRecruit, Long> {
    Optional<ClubRecruit> findById(Long id);
    Optional<ClubRecruit> findByClubId(Long clubId);
    void deleteById(Long id);
}
