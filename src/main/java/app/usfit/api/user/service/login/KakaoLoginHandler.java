package app.usfit.api.user.service.login;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import app.usfit.api.common.enums.AuthProviderEnum;
import app.usfit.api.oauth.kakao.KakaoOAuthClient;
import app.usfit.api.oauth.kakao.dto.KakaoTokenResponse;
import app.usfit.api.oauth.kakao.dto.KakaoUserInfoResponse;
import app.usfit.api.security.jwt.JwtTokenProvider;
import app.usfit.api.user.dto.LoginRequest;
import app.usfit.api.user.dto.LoginResponse;
import app.usfit.api.user.entity.User;
import app.usfit.api.user.repository.UserRepository;

@Component
public class KakaoLoginHandler implements LoginHandler {

    private final UserRepository userRepository;
    private final KakaoOAuthClient kakaoOAuthClient;
    private final JwtTokenProvider jwtTokenProvider;

    public KakaoLoginHandler(
        UserRepository userRepository, 
        KakaoOAuthClient kakaoOAuthClient,
        JwtTokenProvider jwtTokenProvider
    ) {
        this.userRepository = userRepository;
        this.kakaoOAuthClient = kakaoOAuthClient;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Override
    public AuthProviderEnum supports() {
        return AuthProviderEnum.KAKAO;
    }

    @Override
    public LoginResponse handle(LoginRequest request) {
        // 1) accessToken 우선, 없으면 authCode 교환
        String accessToken = request.getAccessToken();
        if (!StringUtils.hasText(accessToken)) {
            if (!StringUtils.hasText(request.getAuthCode())) {
                throw new RuntimeException("카카오 로그인: authCode 또는 accessToken 중 하나는 필수입니다.");
            }
            KakaoTokenResponse tokenResponse = kakaoOAuthClient.exchangeToken(request.getAuthCode());
            accessToken = tokenResponse.accessToken();
        }
        else {
            throw new RuntimeException("카카오 로그인: authCode 또는 accessToken 중 하나는 필수입니다.");
        }

        // 2) 사용자 정보 조회
        KakaoUserInfoResponse profile = kakaoOAuthClient.getUserInfo(accessToken);

        if (profile.id() == null) {
            throw new RuntimeException("카카오 사용자 ID가 비어 있습니다.");
        }
        String providerId = profile.id().toString();
        String email = profile.safeEmail();
        // 카카오는 사용자가 동의하지 않으면 이메일/닉네임이 없을 수 있다고 해서 일단 임시값.
        if (!StringUtils.hasText(email)) email = "kakao_" + providerId + "@placeholder.local";

        // 3) upsert (provider + providerId)
        final String emailFinal = email;
        
        User user = userRepository.findByProviderAndProviderId(AuthProviderEnum.KAKAO, providerId)
                .orElseGet(() -> userRepository.save(User.builder()
                        .email(emailFinal)
                        .provider(AuthProviderEnum.KAKAO)
                        .providerId(providerId)
                        .password(null)
                        .build()));
        // .orElseGet -> 값이 없으면 람다 실행
        // .orElse -> 값이 없든 있는 뒤 먼저 실행
        // .build() -> User 객체 생성

        // 4) 토큰 발행 (jwt)
        String jwt = jwtTokenProvider.createAccessToken(user.getId(), user.getProvider().name(), user.getEmail());
        return LoginResponse.of(user, jwt);
    }
}
