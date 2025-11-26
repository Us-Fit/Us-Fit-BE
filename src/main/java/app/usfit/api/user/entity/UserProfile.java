package app.usfit.api.user.entity;

import java.time.LocalDate;

import org.springframework.data.domain.Persistable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.Data;
import lombok.Getter;

@Entity
@Table(name = "user_profile")
@Data
@Getter
public class UserProfile implements Persistable<Long> {
    @Id
    @Column(name = "user_id")
    private Long userId; // User 엔티티의 ID를 그대로 사용

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "nickname")
    private String nickname; // 닉네임

    @Column(name = "profile_image_key")
    private String profileImageKey; // 프로필 이미지 S3 키

    @Column(name = "gender")
    private boolean gender; // 성별 (true: 남성, false: 여성)

    @Column(name = "height")
    private Double height; // 키 (cm)

    @Column(name = "weight")
    private Double weight; // 몸무게 (kg)

    @Column(name = "birth_date")
    private LocalDate birthDate; // 생년월일 (YYYY-MM-DD)

    

    @Column(name = "preferred_area")
    private String preferredArea; // 선호 운동 지역

    @Column(name = "lat")
    private Double lat; // 위도

    @Column(name = "lng")
    private Double lng; // 경도

    @Column(name = "manner_temp")
    private Double mannerTemp; // 매너 온도

    @Transient
    private boolean isNew = true;

    @Override
    public Long getId() {
        return this.userId;
    }

    @Override
    public boolean isNew() {
        return this.isNew;
    }

    protected UserProfile() {}

    public UserProfile(User user) {
        this.user = user;
        this.userId = user.getId();
    }
    @PostLoad 
    @PostPersist 
    void markNotNew() { this.isNew = false; }
}
