package app.usfit.api.club_recruit.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import app.usfit.api.club_recruit.entity.ClubRecruit;

public interface ClubRecruitRepository extends JpaRepository<ClubRecruit, Long> {
    Optional<ClubRecruit> findById(Long id);
    Optional<ClubRecruit> findByClubId(Long clubId);
    void deleteById(Long id);
}
