package app.usfit.api.RecruitPlayer.dto;

import java.time.LocalDateTime;

import app.usfit.api.RecruitPlayer.entity.RecruitApplication.Status;

public record ApplicationResponse(
    Long applicationId, 
    Long postId,
    Long applicantId, // 유저 아이디임
    String introduction,
    Status status,
    LocalDateTime appliedAt
) {
    
}
