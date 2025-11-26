package app.usfit.api.club.DTO;

import app.usfit.api.user.dto.SimpleProfileResponse;
import lombok.Builder;

@Builder
public record ClubMemberResponse 
(
    Long id,
    Long clubId,
    SimpleProfileResponse userSimpleProfile,
    String role,
    String status,
    String joinedAt
) {

}
