package app.usfit.api.club.DTO;

import app.usfit.api.common.enums.ClubMemberRole;

public record ClubMemberRoleUpdateRequest (
    ClubMemberRole role
) {
    
}
