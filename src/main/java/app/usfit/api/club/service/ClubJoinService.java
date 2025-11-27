package app.usfit.api.club.service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import app.usfit.api.club.DTO.ClubJoinResponse;
import app.usfit.api.club.entity.Club;
import app.usfit.api.club.entity.ClubJoin;
import app.usfit.api.club.entity.ClubMember;
import app.usfit.api.club.repository.ClubJoinRequestRepository;
import app.usfit.api.club.repository.ClubRepository;
import app.usfit.api.user.dto.SimpleProfileResponse;
import app.usfit.api.user.entity.User;
import app.usfit.api.user.service.ProfileService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;


@Service
public class ClubJoinService {
    private final ClubRepository clubRepository;
    private final ClubJoinRequestRepository clubJoinRequestRepository;
    private final ProfileService profileService;
    
    @PersistenceContext
    private EntityManager entityManager;

    public ClubJoinService(ClubRepository clubRepository, ClubJoinRequestRepository clubJoinRequestRepository, ProfileService profileService) {
        this.clubRepository = clubRepository;
        this.clubJoinRequestRepository = clubJoinRequestRepository;
        this.profileService = profileService;
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
                .status("waiting")
                .message(message)
                .build();

        entityManager.persist(jr);

        SimpleProfileResponse userProfile = profileService.getSimpleProfile(requesterId);
        if (userProfile == null) {
            throw new IllegalStateException("사용자 프로필을 찾을 수 없습니다.");
        }
        // 엔티티를 DTO로 매핑하여 반환
        return ClubJoinResponse.builder()
                .requestId(jr.getId())
                .clubId(clubRef.getId())
                .user(userProfile)
                .status(jr.getStatus())
                .message(jr.getMessage())
                .requestedAt(jr.getRequestedAt())
                .build();
    }

    // 동호회의 모든 요청 목록을 확인 (관리자용)
    @Transactional(readOnly = true)
    public List<ClubJoinResponse> listRequestsForClub(Long clubId, Long requesterOwnerId) {
        Club club = entityManager.find(Club.class, clubId);
        if (club == null) {
            throw new IllegalStateException("동호회가 존재하지 않습니다.");
        }
        if (club.getOwner() == null || !club.getOwner().getId().equals(requesterOwnerId)) {
            throw new SecurityException("권한이 없습니다.");
        }

        List<ClubJoin> list = clubJoinRequestRepository.findAllByClub_IdWithUser(clubId);
        
        list.forEach(jr -> {
           System.out.println("[TRACE] ClubJoin id=" + jr.getId() + " user=" + (jr.getUser() == null ? "null" : jr.getUser().getId()));
        });
        
        // 1) 모든 userId 수집
        List<Long> userIds = list.stream()
                .map(jr -> jr.getUser() != null ? jr.getUser().getId() : null)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        userIds.forEach(uid -> {
            System.out.println("[TRACE] Collected userId=" + uid);
        });

        // 2) 한 번에 간단 프로필 조회
        Map<Long, SimpleProfileResponse> profileMap = profileService.getSimpleProfiles(userIds);
        
        if (profileMap != null) {
            profileMap.keySet().forEach(k -> System.out.println("[TRACE] profileMap contains userId=" + k + " -> " + (profileMap.get(k) == null ? "null" : "ok")));
        }

        // 3) 매핑하여 응답 생성
        return list.stream().map(jr -> {
                SimpleProfileResponse userProfile = null;
                var uid = jr.getUser() != null ? jr.getUser().getId() : null;
                System.out.println("[TRACE] mapping ClubJoin id=" + jr.getId() + " uid=" + uid);
                if (uid != null) {
                    userProfile = profileMap.get(uid);
                    if (userProfile == null) {
                        System.out.println("[WARN] profileMap has no entry for userId=" + uid);
                    }
                }

                return ClubJoinResponse.builder()
                        .requestId(jr.getId())
                        .clubId(jr.getClub().getId())
                        .user(userProfile)   // SimpleProfileResponse 삽입
                        .status(jr.getStatus())
                        .message(jr.getMessage())
                        .requestedAt(jr.getRequestedAt())
                        .build();
            }).collect(Collectors.toList());
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

        System.out.println("[TRACE] Deciding request id=" + jr.getId() + " current status=" + jr.getStatus() + " accept=" + accept);

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
        
        SimpleProfileResponse userProfile = profileService.getSimpleProfile(jr.getUser().getId());
        if (userProfile == null) {
            throw new IllegalStateException("사용자 프로필을 찾을 수 없습니다.");
        }

        return ClubJoinResponse.builder()
                .requestId(jr.getId())
                .clubId(club.getId())
                .user(userProfile)
                .status(jr.getStatus())
                .message(jr.getMessage())
                .requestedAt(jr.getRequestedAt())
                .build();
    }
}
