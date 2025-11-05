package app.usfit.api.user.dto;

import app.usfit.api.common.enums.InterestLevel;

public record InterestInput (String code, InterestLevel level) {}
