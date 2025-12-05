package app.usfit.api.RecruitPlayer.dto;

import java.time.LocalDateTime;

import app.usfit.api.RecruitPlayer.entity.RecruitApplication.Status;
import app.usfit.api.user.dto.SimpleProfileResponse;

public record ApplicationPostResponse(
    Long applicationId, 
    RecruitPostResponse post,
    SimpleProfileResponse user, // 유저 아이디임
    String introduction,
    Status status,
    LocalDateTime appliedAt
) {
    
}
