package app.usfit.api.club.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import app.usfit.api.club.entity.ClubJoin;

public interface ClubJoinRequestRepository extends JpaRepository<ClubJoin, Long> {
    boolean existsByClub_IdAndUser_Id(Long clubId, Long userId);
    Optional<ClubJoin> findByClub_IdAndUser_Id(Long clubId, Long userId);

    List<ClubJoin> findAllByClub_Id(Long clubId);

    @Query("SELECT cj FROM ClubJoin cj LEFT JOIN FETCH cj.user WHERE cj.club.id = :clubId")
    List<ClubJoin> findAllByClub_IdWithUser(@Param("clubId") Long clubId);
}
