package app.usfit.api.user.service.login;

import app.usfit.api.common.enums.AuthProviderEnum;
import app.usfit.api.user.dto.LoginRequest;
import app.usfit.api.user.dto.LoginResponse;

public interface LoginHandler {
    AuthProviderEnum supports(); // 어떤 provider 지원하는지 반환
    LoginResponse handle(LoginRequest request); // 로그인 처리
}
