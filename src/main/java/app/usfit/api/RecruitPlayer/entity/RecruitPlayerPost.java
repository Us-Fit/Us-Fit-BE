package app.usfit.api.RecruitPlayer.entity;

import java.time.LocalDateTime;

import app.usfit.api.sport.entity.Sport;
import app.usfit.api.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "recruit_posts")
@Data
public class RecruitPlayerPost {
    @Id
    @GeneratedValue(strategy= GenerationType.IDENTITY)
    @Column(name = "post_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "writer_user_id", nullable = false)
    private User writer; // 작성자 (User 엔티티와 다대일 관계)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sport_type_id", nullable = false) // FK 컬럼명 명시
    private Sport sport;

    private String title; // 제목
    private String description; // 설명

    // 시설 ID로 활동 위치 지정
    @Column(name = "facility_id", nullable = false)
    private Long facilityId; // 장소

    @Column(name = "recruit_deadline", nullable = false)
    private LocalDateTime recruitDeadline;

    @Column(name = "activity_start_time", nullable = false)
    private LocalDateTime activityStartTime;

    @Column(name = "activity_duration_minutes", nullable = false)
    private Integer activityDurationMinutes;

    @Column(name = "max_member", nullable = false)
    private Integer maxMember;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    @Column(name = "sido_nm")
    private String sidoNm; // 활동 시/도 명

    @Column(name = "sigungu_nm")
    private String sigunguNm; // 활동 시/군/구 명
}
