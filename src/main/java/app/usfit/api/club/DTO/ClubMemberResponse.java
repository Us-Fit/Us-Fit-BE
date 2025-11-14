package app.usfit.api.club.DTO;

import lombok.Builder;

@Builder
public record ClubMemberResponse 
(
    Long id,
    Long clubId,
    Long userId,
    String role,
    String status,
    String joinedAt
) {

}
