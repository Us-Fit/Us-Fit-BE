package app.usfit.api.club.DTO;

import app.usfit.api.club.entity.Club;
import app.usfit.api.user.dto.SimpleProfileResponse;

public record ClubCreatedResponse(
    Long id,
    String name,
    String description,
    SimpleProfileResponse owner,
    Long mainFacilityId
) {
    public static ClubCreatedResponse fromEntity(Club club, SimpleProfileResponse ownerProfile) {
        Long mainFacilityId = null;
        try {
            if (club != null && club.getMainFacility() != null) {
                mainFacilityId = club.getMainFacility().getId();
            }
        } catch (Exception ignored) {}

        return new ClubCreatedResponse(
                club != null ? club.getId() : null,
                club != null ? club.getName() : null,
                club != null ? club.getDescription() : null,
                ownerProfile,
                mainFacilityId
        );
    }
}