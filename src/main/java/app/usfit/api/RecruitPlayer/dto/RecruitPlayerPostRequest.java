package app.usfit.api.RecruitPlayer.dto;

import java.time.LocalDateTime;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

// 운동 모집글 작성 요청 DTO
@Data
public class RecruitPlayerPostRequest {
    @NotNull
    private String sportName; // 운동 이름

    @NotNull
    private String title;
    @NotNull
    private String description;
    
    // 활동 위치 -> 시설 id값으로 가져오기
    @NotNull
    private Long facilityId; // 활동 위치 (시설 ID)

    private LocalDateTime recruitDeadline; // 모집 마감 시간
    private LocalDateTime activityStartTime; // 활동 시작 시간
    private Integer activityDurationMinutes; // 활동 시간 (분)

    private int maxMember;
}
