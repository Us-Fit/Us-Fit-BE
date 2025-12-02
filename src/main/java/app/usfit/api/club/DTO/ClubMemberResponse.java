package app.usfit.api.club.DTO;

import app.usfit.api.common.enums.ClubMemberRole;
import app.usfit.api.user.dto.SimpleProfileResponse;
import lombok.Builder;

@Builder
public record ClubMemberResponse 
(
    Long id,
    Long clubId,
    SimpleProfileResponse userSimpleProfile,
    ClubMemberRole role,
    String status,
    String joinedAt
) {

}
