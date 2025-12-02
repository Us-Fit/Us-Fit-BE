package app.usfit.api.club_recruit.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import app.usfit.api.club.entity.Club;
import app.usfit.api.user.entity.User;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "club_recruit")
@Data
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class ClubRecruit {
    @Id
    @Column(name = "club_id")
    private Long id;

    // club을 설정하면 @MapsId가 id 값을 채워줌
    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "club_id")
    private Club club;

    // 작성자(작성자 정보를 user 테이블 FK로 저장)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creator_id", nullable = false)
    private User creator;
    
    @Column(name = "recruit_title", nullable = false, length = 255)
    private String title;

    @Lob
    @Column(name = "recruit_description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // orphanRemoval -> 부모 엔티티에서 자식 엔티티를 제거할 때, 해당 자식 엔티티도 함께 삭제 (cascade랑 비슷한듯..?)
    @OneToMany(mappedBy = "recruit", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC, id ASC")
    @Builder.Default
    private List<ClubRecruitImage> images = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        this.createdAt = this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
