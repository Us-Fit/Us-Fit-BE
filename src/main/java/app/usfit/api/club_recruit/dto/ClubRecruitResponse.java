package app.usfit.api.club_recruit.dto;

import java.util.List;

import app.usfit.api.user.dto.SimpleProfileResponse;

public record ClubRecruitResponse(
    Long id,
    String title,
    String description,
    List<String> imageUrls, // 키가 아닌 URL
    SimpleProfileResponse creator
) {}
