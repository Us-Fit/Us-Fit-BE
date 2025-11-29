package app.usfit.api.RecruitPlayer.dto;

import java.time.LocalDateTime;

import app.usfit.api.user.dto.SimpleProfileResponse;

public record RecruitPlayerPostResponse(
    Long postId,
    SimpleProfileResponse writerUser,
    Long sportTypeId,
    String sportName,
    String title,
    String description,
    LocalDateTime recruitDeadline,
    LocalDateTime activityStartTime,
    Integer activityDurationMinutes,
    Integer maxMember,
    Boolean isActive,

    Long facilityId,
    String sidoNm,
    String sigunguNm
) {}
