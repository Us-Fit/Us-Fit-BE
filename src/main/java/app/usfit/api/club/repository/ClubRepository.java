package app.usfit.api.club.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import app.usfit.api.club.entity.Club;

public interface ClubRepository extends JpaRepository<Club, Long> {
}
