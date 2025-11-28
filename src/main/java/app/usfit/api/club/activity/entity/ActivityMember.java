package app.usfit.api.club.activity.entity;

import app.usfit.api.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class ActivityMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 어떤 활동에 속한 멤버인지
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "activity_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_activity_member_activity"))
    private Activity activity;

    // 어떤 유저인지
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_activity_member_user"))
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private ActivityMemberStatus status; // JOINED, CANCELED, KICKED

    private LocalDateTime joinedAt;

    public void changeStatus(ActivityMemberStatus status) {
        this.status = status;
    }
}
