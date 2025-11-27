package app.usfit.api.user.dto;

import app.usfit.api.common.enums.InterestLevel;

public record InterestInput (
    String sportName, 
    InterestLevel level
) {}
