package app.usfit.api.RecruitPlayer.dto;

import java.time.LocalDateTime;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RecruitPlayerPostRequest {
    @NotNull
    private Long sportTypeId;

    private String title;
    private String description;
      
    private String location;

    private LocalDateTime recruitDeadline; // 모집 마감 시간
    private LocalDateTime activityStartTime; // 활동 시작 시간
    private Integer activityDurationMinutes; // 활동 시간 (분)

    private int maxMember;
}
