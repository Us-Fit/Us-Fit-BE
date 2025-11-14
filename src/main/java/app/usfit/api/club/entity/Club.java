package app.usfit.api.club.entity;

import app.usfit.api.user.entity.User;
import app.usfit.api.facility.entity.Facility;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Club (동호회) 엔티티
 * - 동호회 기본 정보 및 개설자/주 사용 시설을 보관
 * - ClubMember/ClubSport와 1:N 관계
 */
@Entity
@Table(name = "club")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Club {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // 동호회 PK

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_club_owner"))
    private User owner; // 개설자 사용자 FK (users.id)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "main_facility_id",
            foreignKey = @ForeignKey(name = "fk_club_main_facility"))
    private Facility mainFacility; // 주 사용 시설 FK (선택)

    @Column(nullable = false, length = 150)
    private String name; // 동호회 이름

    @Lob
    @Column(columnDefinition = "longtext")
    private String description; // 소개(긴 텍스트 가능)

    @Column(name = "region_code", length = 20)
    private String regionCode; // 활동 지역 코드(시/군/구)

    @Column(name = "region_name", length = 100)
    private String regionName; // 활동 지역 명

    @Column(name = "member_limit")
    private Integer memberLimit; // 최대 인원

    // DB에 tinyint(1)로 되어 있으므로 Boolean으로 매핑
    @Column(name = "visibility", columnDefinition = "tinyint(1)")
    private Boolean visibility; // 공개 여부 (true/false) — 필요시 enum/string으로 바꿀 수 있음

    @Column(length = 20)
    private String status; // 상태 (active/closed 등)

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt; // 생성 시각

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt; // 수정 시각

    @Column(name = "deleted_yn", length = 1)
    private String deletedYn; // 논리삭제 여부 (Y/N)

    @Column(name = "phone_number", length = 20)
    private String phoneNumber; // 연락처

    @Column(name = "sns_link", length = 255)
    private String snsLink; // SNS 링크

    /* =================== 연관관계 (모두 지연 로딩, 소유자는 N 쪽 엔티티) =================== */

    @OneToMany(mappedBy = "club", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ClubSport> sports = new ArrayList<>(); // 동호회-종목 매핑 목록

    @OneToMany(mappedBy = "club", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ClubMember> members = new ArrayList<>(); // 회원 목록

    public void setSports(List<ClubSport> sports) {
        this.sports = sports;
    }
}
