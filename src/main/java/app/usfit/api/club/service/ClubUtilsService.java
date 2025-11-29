package app.usfit.api.club.service;

import org.springframework.stereotype.Service;

import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

@Service
public class ClubUtilsService {
    private final EntityManager entityManager;

    public ClubUtilsService(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    // 동호회 소속인지 확인하는 서비스
    @Transactional
    public boolean isUserInClub(Long clubId, Long userId) {
        // 동호회 멤버 목록에서 현재 사용자 조회
        Long count = entityManager.createQuery(
                "SELECT COUNT(cm) FROM ClubMember cm WHERE cm.club.id = :clubId AND cm.user.id = :userId", Long.class)
                .setParameter("clubId", clubId)
                .setParameter("userId", userId)
                .getSingleResult();

        return count != null && count > 0;
    }

    // 동호회 운영진인지 확인하는 서비스
    @Transactional
    public boolean isUserClubAdmin(Long clubId, Long userId) {
        Long count = entityManager.createQuery(
                "SELECT COUNT(cm) FROM ClubMember cm WHERE cm.club.id = :clubId AND cm.user.id = :userId AND cm.role IN ('owner', 'admin')", Long.class)
                .setParameter("clubId", clubId)
                .setParameter("userId", userId)
                .getSingleResult();

        return count != null && count > 0;

    }
}
