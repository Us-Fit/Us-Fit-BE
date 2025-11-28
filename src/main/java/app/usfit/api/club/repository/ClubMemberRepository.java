package app.usfit.api.club.repository;

import app.usfit.api.club.entity.ClubMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ClubMemberRepository extends JpaRepository<ClubMember, Long> {

    Optional<ClubMember> findByClubIdAndUserId(Long clubId, Long userId);
}
