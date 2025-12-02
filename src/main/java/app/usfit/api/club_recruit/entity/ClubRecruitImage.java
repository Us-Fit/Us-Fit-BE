package app.usfit.api.club_recruit.entity;

import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "club_recruit_image")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClubRecruitImage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "club_recruit_id", nullable = false)
    private ClubRecruit recruit;

    @Column(name = "image_key", nullable = false, length = 512)
    private String imageKey;

    @Column(name = "image_description", length = 512)
    private String description;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
