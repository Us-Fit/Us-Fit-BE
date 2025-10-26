package app.usfit.api.RecruitPlayer.entity;

import app.usfit.api.user.entity.User;
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
    private Long id;

    private String sportType; // 운동 종류 
    private int maxMemberCount; // 모집 인원
    private int currMemeberCount; // 현재 인원
    private String location; // 장소
    private String scheduledTime; // 예정 시간
    private String title; // 제목
    private String description; // 설명
    private boolean isActive; // 모집 상태 (활성/비활성)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User writer; // 작성자 (User 엔티티와 다대일 관계)
}
