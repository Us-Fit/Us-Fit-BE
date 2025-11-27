package app.usfit.api.club.DTO;

import app.usfit.api.user.dto.SimpleProfileResponse;

public record ClubCreatedResponse(
    Long id,
    String name,
    String description,
    SimpleProfileResponse owner,
    Long mainFacilityId
) {}