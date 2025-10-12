package app.usfit.api.sport.entity;

import app.usfit.api.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_sport",
        indexes = {
                @Index(name = "idx_user_sport_user", columnList = "user_id"),
                @Index(name = "idx_user_sport_sport", columnList = "sport_id")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_user_sport_user_sport", columnNames = {"user_id", "sport_id"})
        })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class UserSport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;                 // PK

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_user_sport_user"))
    private User user;               // 사용자 FK

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sport_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_user_sport_sport"))
    private Sport sport;             // 종목 FK

    @Column(nullable = false)
    private int level;               // 종목별 숙련도

    @Column(nullable = false)
    private int preference;          // 선호도(1~5 등)

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt; // 생성 시각 (DEFAULT CURRENT_TIMESTAMP)
}
