package app.usfit.api.club.activity.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@NoArgsConstructor
@Schema(description = "동호회 활동 생성 요청 DTO")
public class ActivityCreateRequest {

    @Schema(description = "활동 제목", example = "주말 농구 번개")
    private String title;

    @Schema(description = "활동 설명", example = "초보~중급 환영! 농구 후 저녁식사 예정입니다.")
    private String description;

    @Schema(description = "활동 날짜", example = "2025-12-01")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate activityDate;

    @Schema(description = "시작 시간", example = "19:00:00")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm:ss")
    private LocalTime startTime;

    @Schema(description = "종료 시간", example = "21:00:00")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm:ss")
    private LocalTime endTime;

    @Schema(description = "모집 인원 수(최대)", example = "10")
    private int maxPeople;

    @Schema(description = "주 사용 시설 ID (선택)", example = "123")
    private Long actFacilityId;

    @Schema(description = "활동 장소 이름(직접 입력 시)", example = "명지대 자연캠퍼스 농구장")
    private String locationName;
}
