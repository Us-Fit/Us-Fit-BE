package app.usfit.api.club.entity;

import app.usfit.api.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * ClubMember (동호회 회원) 엔티티
 * - (club_id, user_id) 조합 유니크 보장 → 중복 가입 방지
 */
@Entity
@Table(
        name = "club_member",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_club_member",
                columnNames = {"club_id", "user_id"}
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
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

    @Column(length = 20)
    private String role;   // 역할: owner/admin/member 등

    @Column(length = 20)
    private String status; // 상태: active/pending/banned/left 등

    @CreationTimestamp
    private LocalDateTime joinedAt; // 가입 시각 (INSERT 시 자동 기록)

    private LocalDateTime leftAt;   // 탈퇴 시각 (선택)
}
