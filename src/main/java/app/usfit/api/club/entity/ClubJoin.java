package app.usfit.api.club.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import app.usfit.api.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "club_join_requests")
@Data
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
public class ClubJoin {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "club_id", nullable = false,
        foreignKey = @ForeignKey(name = "fk_club_join_request_club"))
    private Club club;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false,
        foreignKey = @ForeignKey(name = "fk_club_join_request_user"))
    private User user;

    @Column(name = "status", length = 20)
    private String status; // e.g. pending/approved/rejected

    @Column(name = "message", length = 512)
    private String message; // 한줄 소개

    @CreationTimestamp
    private LocalDateTime requestedAt;
}
