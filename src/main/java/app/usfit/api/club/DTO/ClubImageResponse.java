package app.usfit.api.club.DTO;

import java.time.LocalDateTime;

import app.usfit.api.user.dto.SimpleProfileResponse;

public record ClubImageResponse(
    String imageUrl,
    SimpleProfileResponse uploader,
    LocalDateTime uploadTime,
    String title,
    String description
) {}
