package app.usfit.api.user.dto;

import app.usfit.api.common.enums.InterestLevel;

public record InterestOutput(
    String name,            // 스포츠 표시 이름(없으면 null)
    InterestLevel level
) {}
