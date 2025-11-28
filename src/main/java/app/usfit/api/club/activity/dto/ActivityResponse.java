package app.usfit.api.club.activity.dto;

import app.usfit.api.club.activity.entity.Activity;
import app.usfit.api.club.activity.entity.ActivityStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Builder
public class ActivityResponse {

    private Long id;
    private Long clubId;

    private String title;
    private String description;

    private LocalDate activityDate;
    private LocalTime startTime;
    private LocalTime endTime;

    private int maxPeople;
    private int currentPeople;

    private Long actFacilityId;
    private String locationName;

    private ActivityStatus status;

    public static ActivityResponse from(Activity activity) {
        return ActivityResponse.builder()
                .id(activity.getId())
                .clubId(activity.getClub().getId())
                .title(activity.getTitle())
                .description(activity.getDescription())
                .activityDate(activity.getActivityDate())
                .startTime(activity.getStartTime())
                .endTime(activity.getEndTime())
                .maxPeople(activity.getMaxPeople())
                .currentPeople(activity.getCurrentPeople())
                .actFacilityId(
                        activity.getActFacility() != null
                                ? activity.getActFacility().getId()
                                : null
                )
                .locationName(activity.getLocationName())
                .status(activity.getStatus())
                .build();
    }
}
