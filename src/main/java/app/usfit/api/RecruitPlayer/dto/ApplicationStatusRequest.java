package app.usfit.api.RecruitPlayer.dto;

import app.usfit.api.RecruitPlayer.entity.RecruitApplication;

public record ApplicationStatusRequest(
    RecruitApplication.Status status
) {
    
}
