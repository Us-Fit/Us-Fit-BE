package app.usfit.api.club.DTO;

import java.util.List;

import app.usfit.api.user.dto.SimpleProfileResponse;

public record ClubSimpleInfoResponse(
    Long id,
    String name,
    String regionName,
    Integer memberLimit,
    Boolean visibility,
    String status,
    SimpleProfileResponse owner,
    Long mainFacilityId,
    Integer memberCount,
    List<ClubSportResponse> sports
) {}
