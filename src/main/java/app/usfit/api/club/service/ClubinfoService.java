package app.usfit.api.club.service;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import app.usfit.api.club.DTO.ClubDetailInfoResponse;
import app.usfit.api.club.DTO.ClubMemberResponse;
import app.usfit.api.club.DTO.ClubSportResponse;
import app.usfit.api.club.entity.Club;
import app.usfit.api.club.entity.ClubMember;
import app.usfit.api.facility.dto.FacilityDetailView;
import app.usfit.api.facility.repository.FacilityRepository;
import app.usfit.api.user.dto.SimpleProfileResponse;
import app.usfit.api.user.service.ProfileService;
import jakarta.transaction.Transactional;

@Service
public class ClubinfoService {
    private final jakarta.persistence.EntityManager entityManager;
    private final FacilityRepository facilityRepository;
    private final ProfileService profileService;
    
    public ClubinfoService(jakarta.persistence.EntityManager entityManager, FacilityRepository facilityRepository, ProfileService profileService) {
        this.entityManager = entityManager;
        this.facilityRepository = facilityRepository;
        this.profileService = profileService;
    }

    // 특정 동호회 정보 상세 조회
    @Transactional
    public ClubDetailInfoResponse getClubDetail(Long clubId) {
        Club club = entityManager.find(Club.class, clubId);
        if (club == null) {
            throw new IllegalStateException("동호회가 존재하지 않습니다.");
        }

        // 이미 URL을 Club에 저장해두었으므로 직접 사용
        String imageUrl = null;
        try {
            imageUrl = club.getClubMainImageUrl();
        } catch (Throwable ignored) {
            imageUrl = null;
        }

        // member count - JPQL count로 안전하게 계산
        int currMemberCount = club.getMembers() != null ? club.getMembers().size() : 0;

        // facility -> projection (FacilityDetailView) 사용
        FacilityDetailView facilityView = null;
        try {
            if (club.getMainFacility() != null && club.getMainFacility().getId() != null) {
                Long mainFacilityId = club.getMainFacility().getId();
                facilityView = facilityRepository.findProjectedById(mainFacilityId).orElse(null);
            }
        } catch (Throwable ignored) {
            facilityView = null;
        }

        // sports -> DTO (트랜잭션 내에서 안전하게 lazy 접근)
        List<ClubSportResponse> sports = List.of();
        try {
            if (club.getSports() != null) {
                sports = club.getSports().stream()
                        .map(s -> new ClubSportResponse(
                                s.getId(),
                                s.getSport() != null ? s.getSport().getName() : null
                        ))
                        .collect(Collectors.toList());
            }
        } catch (Throwable ignored) {
            sports = List.of();
        }

        // created date
        LocalDate createdAt = null;
        try {
            if (club.getCreatedAt() != null) {
                createdAt = club.getCreatedAt().toLocalDate();
            }
        } catch (Throwable ignored) {
            createdAt = null;
        }

        return new ClubDetailInfoResponse(
                club.getName(),
                club.getDescription(),
                imageUrl,
                currMemberCount,
                club.getMemberLimit(),
                club.getPhoneNumber(),
                createdAt,
                club.getSnsLink(),
                facilityView,
                sports
        );
    }

    // 동호회 모든 동호회원 조회
    @Transactional
    public List<ClubMemberResponse> listClubMemebers(Long clubId) {
        List<ClubMember> members = entityManager.createQuery(
                "SELECT cm FROM ClubMember cm WHERE cm.club.id = :clubId", ClubMember.class)
                .setParameter("clubId", clubId)
                .getResultList();
            
        return members.stream().map(cm -> {
                    // userId로 간단 프로필 조회
                    SimpleProfileResponse simple = null;
                    try {
                        simple = profileService.getSimpleProfile(cm.getUser().getId());
                    } catch (Exception ignored) {
                        // 프로필 조회 실패 시 null 허용
                    }

                    return new ClubMemberResponse(
                            cm.getId(),
                            cm.getClub().getId(),
                            simple,                     // 기존 userId 대신 SimpleProfileResponse
                            cm.getRole(),
                            cm.getStatus(),
                            cm.getJoinedAt().toString()
                    );
                }).toList();
                
    }

    // 동호회원 역할 변경
    @Transactional
    public ClubMemberResponse changeMemberRole(Long clubId, Long memberId, Long ownerId, String newRole) {
        Club club = entityManager.find(Club.class, clubId);
        if (club == null) throw new IllegalStateException("동호회가 존재하지 않습니다.");
        if (club.getOwner() == null || !club.getOwner().getId().equals(ownerId)) {
            throw new IllegalStateException("동호회 소유자만 동호회원의 역할을 변경할 수 있습니다.");
        }

        ClubMember cm = entityManager.find(ClubMember.class, memberId);

        if (cm == null) throw new IllegalStateException("동호회원이 존재하지 않습니다.");
        if (!cm.getClub().getId().equals(clubId)) throw new IllegalStateException("해당 동호회의 동호회원이 아닙니다.");
        if ("owner".equalsIgnoreCase(cm.getRole())) throw new IllegalStateException("동호회 소유자의 역할은 변경할 수 없습니다.");

        cm.setRole(newRole);
        entityManager.merge(cm);

        var simpleProfile = profileService.getSimpleProfile(cm.getUser().getId());
        return ClubMemberResponse.builder()
                .id(cm.getId())
                .clubId(cm.getClub().getId())
                .userSimpleProfile(simpleProfile)
                .role(cm.getRole())
                .status(cm.getStatus())
                .joinedAt(cm.getJoinedAt().toString())
                .build();
    }

    // 동호회원 탈퇴 -> 관리자가 탈퇴 처리
    /**
     * 운영자(소유자/관리자)가 특정 멤버를 강제 탈퇴시킴
     * - owner: member, admin 삭제 가능(다른 owner 삭제 불가)
     * - admin: member만 삭제 가능
     * - 자기 자신 강제 삭제 불가 (자기 탈퇴 API 사용)
     */
    @Transactional
    public void removeMember(Long clubId, Long memberId, Long requesterId) {
        if (memberId.equals(requesterId)) {
            throw new IllegalStateException("자기 자신을 강제 탈퇴시킬 수 없습니다.");
        }
        // 동호회 여부 확인
        Club club = entityManager.find(Club.class, clubId);
        if (club == null) throw new IllegalStateException("동호회가 존재하지 않습니다.");

        ClubMember user = entityManager.createQuery(
                "SELECT cm FROM ClubMember cm WHERE cm.club.id = :clubId AND cm.user.id = :userId", ClubMember.class)
                .setParameter("clubId", clubId)
                .setParameter("userId", memberId)
                .setMaxResults(1)
                .getResultStream()
                .findFirst()
                .orElse(null);
        if (user == null) throw new IllegalStateException("동호회원이 존재하지 않습니다.");
        
        ClubMember target = entityManager.createQuery(
                "SELECT cm FROM ClubMember cm WHERE cm.club.id = :clubId AND cm.user.id = :userId", ClubMember.class)
                .setParameter("clubId", clubId)
                .setParameter("userId", requesterId)
                .setMaxResults(1)
                .getResultStream()
                .findFirst()
                .orElse(null);
        if (target == null) throw new IllegalStateException("대상이 동호회원이 아닙니다.");

        // 권한 확인
        String userRole = user.getRole() == null ? "member" : user.getRole().toLowerCase();
        String targetRole = target.getRole() == null ? "member" : target.getRole().toLowerCase();

        // 권한 검증
        if ("owner".equals(userRole)) {
            // 혹시 몰라서 넣어둠. owner는 owner 삭제 불가
            if ("owner".equals(targetRole)) {
                throw new IllegalStateException("다른 소유자를 삭제할 수 없습니다.");
            }
        }
        else if ("admin".equals(userRole)) {
            if (!"member".equals(targetRole)) {
                throw new IllegalStateException("관리자는 일반 멤버만 강제 탈퇴시킬 수 있습니다.");
            }
        }
        else {
            throw new IllegalStateException("운영자(소유자/관리자)만 동호회원 강제 탈퇴가 가능합니다.");
        }

        entityManager.remove(target);
    }

    // 동호회원 본인 탈퇴
    @Transactional
    public void leaveClub(Long clubId, Long userId) {
        Club club = entityManager.find(Club.class, clubId);
        if (club == null) throw new IllegalStateException("동호회가 존재하지 않습니다.");
        ClubMember cm = entityManager.createQuery(
                "SELECT cm FROM ClubMember cm WHERE cm.club.id = :clubId AND cm.user.id = :userId", ClubMember.class)
                .setParameter("clubId", clubId)
                .setParameter("userId", userId)
                .getResultStream()
                .findFirst()
                .orElse(null);

        
        if (cm == null) throw new IllegalStateException("동호회원이 존재하지 않습니다.");
        if (cm.getRole().equalsIgnoreCase("owner")) throw new IllegalStateException("동호회 소유자는 탈퇴할 수 없습니다.");

        entityManager.remove(cm);
    }

    // 동호회 소유 권한 넘기기
    // 기존 소유자는 자동으로 일반 멤버가 됨
    @Transactional
    public void transferOwnershop(Long clubId, Long userId, Long newOwnerId) {
        Club club = entityManager.find(Club.class, clubId);
        if (club == null) throw new IllegalStateException("동호회가 존재하지 않습니다.");
        // 소유자 확인
        if (club.getOwner() == null || !club.getOwner().getId().equals(userId)) {
            throw new IllegalStateException("동호회 소유자만 소유권을 이전할 수 있습니다.");
        }
        // 본인에게 이전 불가
        if (userId.equals(newOwnerId)) {
            throw new IllegalStateException("본인에게 소유권을 이전할 수 없습니다.");
        }
        // 새 소유자가 동호회원인지 확인
        ClubMember newOwnerMember = entityManager.createQuery(
                "SELECT cm FROM ClubMember cm WHERE cm.club.id = :clubId AND cm.user.id = :newOwnerId", ClubMember.class)
                .setParameter("clubId", clubId)
                .setParameter("newOwnerId", newOwnerId)
                .getResultStream()
                .findFirst()
                .orElse(null);

        if (newOwnerMember == null) throw new IllegalStateException("새 소유자가 동호회원이 아닙니다.");

        // 기존 소유자 역할 변경
        ClubMember currentOwnerMember = entityManager.createQuery(
                "SELECT cm FROM ClubMember cm WHERE cm.club.id = :clubId AND cm.user.id = :userId", ClubMember.class)
                .setParameter("clubId", clubId)
                .setParameter("userId", userId)
                .getSingleResult();
        
        // 기존 소유자 역할을 admin으로 변경
        currentOwnerMember.setRole("admin");
        entityManager.merge(currentOwnerMember);

        // 새 소유자 역할 변경
        newOwnerMember.setRole("owner");
        entityManager.merge(newOwnerMember);

        // 동호회 소유자 변경
        club.setOwner(newOwnerMember.getUser());
        entityManager.merge(club);
    }

    // 소유자가 admin권리 부여
    @Transactional
    public void grantAdminRole(Long clubId, Long ownerId, Long memberId) {
        Club club = entityManager.find(Club.class, clubId);
        if (club == null) throw new IllegalStateException("동호회가 존재하지 않습니다.");

        if (club.getOwner() == null || !club.getOwner().getId().equals(ownerId)) {
            throw new IllegalStateException("소유자만 관리자 권한을 부여할 수 있습니다.");
        }

        ClubMember target = entityManager.createQuery(
                "SELECT cm FROM ClubMember cm WHERE cm.club.id = :clubId AND cm.user.id = :userId", ClubMember.class)
                .setParameter("clubId", clubId)
                .setParameter("userId", memberId)
                .setMaxResults(1)
                .getResultStream()
                .findFirst()
                .orElse(null);

        if (target == null) throw new IllegalStateException("대상 사용자가 동호회원이 아닙니다.");

        // 이미 owner이면 권한 변경 불필요
        if ("owner".equalsIgnoreCase(target.getRole())) {
            throw new IllegalStateException("소유자에게는 관리자 권한을 부여할 수 없습니다.");
        }

        target.setRole("admin");
        entityManager.merge(target);
    }

    // 소유자가 admin권리 박탈
    @Transactional
    public void revokeAdminRole(Long clubId, Long ownerId, Long memberId) {
        Club club = entityManager.find(Club.class, clubId);
        if (club == null) throw new IllegalStateException("동호회가 존재하지 않습니다.");

        if (club.getOwner() == null || !club.getOwner().getId().equals(ownerId)) {
            throw new IllegalStateException("소유자만 관리자 권한을 박탈할 수 있습니다.");
        }

        ClubMember target = entityManager.createQuery(
                "SELECT cm FROM ClubMember cm WHERE cm.club.id = :clubId AND cm.user.id = :userId", ClubMember.class)
                .setParameter("clubId", clubId)
                .setParameter("userId", memberId)
                .setMaxResults(1)
                .getResultStream()
                .findFirst()
                .orElse(null);

        if (target == null) throw new IllegalStateException("대상 사용자가 동호회원이 아닙니다.");

        // 이미 owner이면 권한 변경 불필요
        if ("owner".equalsIgnoreCase(target.getRole())) {
            throw new IllegalStateException("소유자의 관리자 권한은 박탈할 수 없습니다.");
        }

        target.setRole("member");
        entityManager.merge(target);
    }
}
