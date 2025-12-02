package app.usfit.api.club.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import app.usfit.api.common.enums.ClubMemberRole;
import app.usfit.api.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * ClubMember (동호회 회원) 엔티티
 * - (club_id, user_id) 조합 유니크 보장 → 중복 가입 방지
 */
@Entity
@Table(
        name = "club_member",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_club_member",
                columnNames = {"club_id", "user_id"} //unique 유지하기 위함
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Setter
public class ClubMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // Surrogate Key

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "club_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_club_member_club"))
    private Club club; // 동호회 FK

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_club_member_user"))
    private User user; // 회원 사용자 FK

    @Column(name = "role", length = 20)
    @Enumerated(EnumType.STRING)
    private ClubMemberRole role;   // 역할: owner/admin/member 등

    @Column(name = "status", length = 20)
    private String status; // 상태: active/pending/banned/left 등

    @CreationTimestamp
    private LocalDateTime joinedAt; // 가입 시각 (INSERT 시 자동 기록)

    private LocalDateTime leftAt;   // 탈퇴 시각 (선택)
}
