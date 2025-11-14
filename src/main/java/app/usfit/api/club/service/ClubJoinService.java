package app.usfit.api.club.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import app.usfit.api.club.DTO.ClubJoinResponse;
import app.usfit.api.club.entity.Club;
import app.usfit.api.club.entity.ClubJoin;
import app.usfit.api.club.entity.ClubMember;
import app.usfit.api.club.repository.ClubJoinRequestRepository;
import app.usfit.api.club.repository.ClubRepository;
import app.usfit.api.user.entity.User;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;

@Service
public class ClubJoinService {
    private final ClubRepository clubRepository;
    private final ClubJoinRequestRepository clubJoinRequestRepository;
    
    @PersistenceContext
    private EntityManager entityManager;

    public ClubJoinService(ClubRepository clubRepository, ClubJoinRequestRepository clubJoinRequestRepository) {
        this.clubRepository = clubRepository;
        this.clubJoinRequestRepository = clubJoinRequestRepository;
    }

    @Transactional
    public ClubJoinResponse createJoinRequest(Long clubId, Long requesterId, String message) {
        // 중복 요청 방지 (간단히 존재 여부 체크)
        if (clubJoinRequestRepository.existsByClub_IdAndUser_Id(clubId, requesterId)) {
            throw new IllegalStateException("이미 가입 요청이 존재합니다.");
        }

        Club clubRef = entityManager.getReference(Club.class, clubId);
        User requester = entityManager.find(User.class, requesterId);

        ClubJoin jr = ClubJoin.builder()
                .club(clubRef)
                .user(requester)
                .status("wating")
                .message(message)
                .build();

        entityManager.persist(jr);
        // 엔티티를 DTO로 매핑하여 반환
        return ClubJoinResponse.builder()
                .id(jr.getId())
                .clubId(clubRef.getId())
                .userId(requester.getId())
                .status(jr.getStatus())
                .message(jr.getMessage())
                .requestedAt(jr.getRequestedAt())
                .build();
    }

    // 동호회의 모든 요청 목록을 확인 (관리자용)
    @Transactional
    public List<ClubJoinResponse> listRequestsForClub(Long clubId, Long requesterOwnerId) {
        Club club = entityManager.find(Club.class, clubId);
        if (club == null) {
            throw new IllegalStateException("동호회가 존재하지 않습니다.");
        }
        if (club.getOwner() == null || !club.getOwner().getId().equals(requesterOwnerId)) {
            throw new SecurityException("권한이 없습니다.");
        }

        List<ClubJoin> list = clubJoinRequestRepository.findAllByClub_Id(clubId);
        return list.stream().map(jr -> ClubJoinResponse.builder()
                .id(jr.getId())
                .clubId(jr.getClub().getId())
                .userId(jr.getUser().getId())
                .status(jr.getStatus())
                .message(jr.getMessage())
                .requestedAt(jr.getRequestedAt())
                .build()).collect(Collectors.toList());
    }

    // 동호회장이 가입 요청 수락/거절
    @Transactional
    public ClubJoinResponse decideRequest(Long requestId, Long ownerId, boolean accept) {
        ClubJoin jr = clubJoinRequestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalStateException("요청을 찾을 수 없습니다."));

        Club club = jr.getClub();
        if (club == null) {
            throw new IllegalStateException("연결된 동호회가 없습니다.");
        }
        if (club.getOwner() == null || !club.getOwner().getId().equals(ownerId)) {
            throw new SecurityException("권한이 없습니다.");
        }

        if (!"waiting".equalsIgnoreCase(jr.getStatus())) {
            throw new IllegalStateException("이미 처리된 요청입니다.");
        }

        if (accept) {
            // 멤버 추가
            ClubMember member = ClubMember.builder()
                    .club(club)
                    .user(jr.getUser())
                    .role("member")
                    .status("active")
                    .build();
            entityManager.persist(member);
            jr.setStatus("approved");
        } else {
            jr.setStatus("rejected");
        }

        // 변경 반영
        entityManager.merge(jr);

        return ClubJoinResponse.builder()
                .id(jr.getId())
                .clubId(club.getId())
                .userId(jr.getUser().getId())
                .status(jr.getStatus())
                .message(jr.getMessage())
                .requestedAt(jr.getRequestedAt())
                .build();
    }
}
