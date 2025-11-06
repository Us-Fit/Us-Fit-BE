package app.usfit.api.RecruitPlayer.dto;

import java.time.LocalDateTime;

public record RecruitPlayerPostResponse(
    Long postId,
    Long writerUserId,
    Long sportTypeId,
    String title,
    String description,
    String location,
    LocalDateTime recruitDeadline,
    LocalDateTime activityStartTime,
    Integer activityDurationMinutes,
    Integer maxMember,
    Boolean isActive
) {}
