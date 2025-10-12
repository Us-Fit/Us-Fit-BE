package app.usfit.api.user.dto;

import app.usfit.api.common.enums.AuthProviderEnum;
import app.usfit.api.user.entity.User;

/**
 * 로그인 성공시 내려줄 응답 DTO
 * token 은 JWT 발급 적용 후 채워질 예정 (현재 null 가능)
 */

// record: 불변 객체, getter, toString, equals, hashCode 자동 생성
public record LoginResponse(
        Long userId,
        String email,
        String nickname,
        AuthProviderEnum provider,
        String token
) {
    public static LoginResponse of(User user, String token) {
        return new LoginResponse(
                user.getId(),
                user.getEmail(),
                user.getNickname(),
                user.getProvider(),
                token
        );
    }
}
