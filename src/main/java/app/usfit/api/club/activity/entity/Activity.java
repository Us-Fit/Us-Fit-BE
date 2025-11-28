package app.usfit.api.club.activity.entity;

import app.usfit.api.club.entity.Club;
import app.usfit.api.facility.entity.Facility;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Activity {

    @Id @GeneratedValue
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    private Club club;  // 어떤 club에서 만든 활동인지

    private String title;
    private String description;

    private LocalDate activityDate;
    private LocalTime startTime;
    private LocalTime endTime;

    private int maxPeople;
    private int currentPeople;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "act_facility_id",
            foreignKey = @ForeignKey(name = "fk_activity_act_facility"))
    private Facility actFacility; // 주 사용 시설 FK (선택)

    private String locationName;

    @Enumerated(EnumType.STRING)
    private ActivityStatus status; // OPEN, CLOSED, CANCELLED

    public void changeStatus(ActivityStatus status) {
        this.status = status;
    }

    public void updateBasicInfo(
            String title,
            String description,
            LocalDate activityDate,
            LocalTime startTime,
            LocalTime endTime,
            int maxPeople,
            String locationName
    ) {
        this.title = title;
        this.description = description;
        this.activityDate = activityDate;
        this.startTime = startTime;
        this.endTime = endTime;
        this.maxPeople = maxPeople;
        this.locationName = locationName;
    }

    public void increaseCurrentPeople() {
        this.currentPeople++;
    }

    public void decreaseCurrentPeople() {
        if (this.currentPeople > 0) {
            this.currentPeople--;
        }
    }

    public boolean isExpired() {
        if (activityDate == null) return false;

        // endTime 없으면 하루 끝까지 열려 있다고 가정
        LocalTime end = (endTime != null) ? endTime : LocalTime.MAX;

        LocalDateTime activityEnd = LocalDateTime.of(activityDate, end);
        return LocalDateTime.now().isAfter(activityEnd);
    }
}
