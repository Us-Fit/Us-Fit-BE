package app.usfit.api.club.service;

import java.util.List;

import org.springframework.stereotype.Service;

import app.usfit.api.club.DTO.ClubMemberResponse;
import app.usfit.api.club.entity.Club;
import app.usfit.api.club.entity.ClubMember;
import jakarta.transaction.Transactional;

@Service
public class ClubinfoService {
    private final jakarta.persistence.EntityManager entityManager;
    public ClubinfoService(jakarta.persistence.EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    // 동호회 모든 동호회원 조회
    @Transactional
    public List<ClubMemberResponse> listClubMemebers(Long clubId) {
        List<ClubMember> members = entityManager.createQuery(
                "SELECT cm FROM ClubMember cm WHERE cm.club.id = :clubId", ClubMember.class)
                .setParameter("clubId", clubId)
                .getResultList();
            
        return members.stream().map(cm -> new ClubMemberResponse(
                    cm.getId(),
                    cm.getClub().getId(),
                    cm.getUser().getId(),
                    cm.getRole(),
                    cm.getStatus(),
                    cm.getJoinedAt().toString()
                )).toList();
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

        return ClubMemberResponse.builder()
                .id(cm.getId())
                .clubId(cm.getClub().getId())
                .userId(cm.getUser().getId())
                .role(cm.getRole())
                .status(cm.getStatus())
                .joinedAt(cm.getJoinedAt().toString())
                .build();
    }

    // 동호회원 탈퇴
    @Transactional
    public void removeMember(Long clubId, Long memberId, Long requesterId) {
        Club club = entityManager.find(Club.class, clubId);
        if (club == null) throw new IllegalStateException("동호회가 존재하지 않습니다.");
        if (club.getOwner() == null || !club.getOwner().getId().equals(requesterId)) {
            throw new IllegalStateException("동호회 소유자만 동호회원의 탈퇴를 처리할 수 있습니다.");
        }

        ClubMember cm = entityManager.find(ClubMember.class, memberId);
        if (cm == null) throw new IllegalStateException("동호회원이 존재하지 않습니다.");
        if (!cm.getClub().getId().equals(clubId)) throw new IllegalStateException("해당 동호회의 동호회원이 아닙니다.");
        if (cm.getRole().equalsIgnoreCase("owner")) throw new IllegalStateException("동호회 소유자는 탈퇴할 수 없습니다.");

        entityManager.remove(cm);
    }
}
