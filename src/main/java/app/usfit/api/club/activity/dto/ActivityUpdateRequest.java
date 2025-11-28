package app.usfit.api.club.activity.dto;

import app.usfit.api.club.activity.entity.ActivityStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@NoArgsConstructor
@Schema(description = "동호회 활동 수정 요청 DTO")
public class ActivityUpdateRequest {

    @Schema(description = "활동 제목", example = "주말 농구 정기모임")
    private String title;

    @Schema(description = "활동 설명", example = "중급 이상을 대상으로 변경합니다.")
    private String description;

    @Schema(description = "활동 날짜", example = "2025-12-02")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate activityDate;

    @Schema(description = "시작 시간", example = "19:00:00")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm:ss")
    private LocalTime startTime;

    @Schema(description = "종료 시간", example = "21:00:00")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm:ss")
    private LocalTime endTime;

    @Schema(description = "모집 인원 최대치", example = "12")
    private int maxPeople;

    @Schema(description = "활동 시설 ID (선택)", example = "123")
    private Long actFacilityId;

    @Schema(description = "직접 입력한 위치명(시설 사용 안 할 시)", example = "명지대 자연캠 농구장 A코트")
    private String locationName;

    @Schema(description = "활동 상태", example = "OPEN")
    private ActivityStatus status;
}
