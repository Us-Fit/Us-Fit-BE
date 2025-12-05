package app.usfit.api.RecruitPlayer.dto;

public record RecruitPostResponse (
    String sportName, // 운동 이름
    String title,
    String description,
    Long facilityId, // 활동 위치 (시설 ID)
    String sidoNm, // 활동 시/도 명
    String sigunguNm, // 활동 시/군/구 명
    Integer activityDurationMinutes // 활동 시간 (분)
){}
