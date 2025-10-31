package app.usfit.api.RecruitPlayer.dto;

import java.time.LocalDateTime;

import app.usfit.api.RecruitPlayer.entity.RecruitApplication.Status;

public record ApplicationResponse(
    Long applicationId,
    Long postId,
    Long applicantId,
    String introduction,
    Status status,
    LocalDateTime appliedAt
) {
    
}
