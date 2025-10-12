package app.usfit.api.user.dto;

import app.usfit.api.common.enums.AuthProviderEnum;
import lombok.Data;

@Data
public class LoginRequest {
    private AuthProviderEnum provider; // LOCAL, KAKAO, GOOGLE
    private String email;              // LOCAL 용
    private String password;           // LOCAL 용
    private String authCode;           // OAuth2 code (카카오/구글)
    private String accessToken;        // 이미 프론트에서 받은 토큰 전달 시
}
