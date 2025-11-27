package app.usfit.api.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record SimpleProfileResponse(
    @Schema(description = "사용자 ID", example = "12") Long userId,
    @Schema(description = "닉네임", example = "yoon") String nickname,
    @Schema(description = "프로필 이미지 URL", example = "https://.../img.jpg") String profileImageUrl,
    @Schema(description = "성별", example = "true") Boolean gender
) {}
