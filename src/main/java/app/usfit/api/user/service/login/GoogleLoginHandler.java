package app.usfit.api.user.service.login;

import org.springframework.stereotype.Component;

import app.usfit.api.common.enums.AuthProviderEnum;
import app.usfit.api.user.dto.LoginRequest;
import app.usfit.api.user.dto.LoginResponse;
import app.usfit.api.user.entity.User;
import app.usfit.api.user.repository.UserRepository;

@Component
public class GoogleLoginHandler implements LoginHandler {

    private final UserRepository userRepository;

    public GoogleLoginHandler(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public AuthProviderEnum supports() {
        return AuthProviderEnum.GOOGLE;
    }

    @Override
    public LoginResponse handle(LoginRequest request) {
        // TODO: authCode -> token -> google profile 연동 로직
        String mockProviderId = "google-" + (request.getAuthCode() != null ? request.getAuthCode() : "temp");
        User user = userRepository.findByProviderAndProviderId(AuthProviderEnum.GOOGLE, mockProviderId)
                .orElseGet(() -> userRepository.save(User.builder()
                        .email("google_" + mockProviderId + "@placeholder.local")
                        .provider(AuthProviderEnum.GOOGLE)
                        .providerId(mockProviderId)
                        .password(null)
                        .build()));
        return LoginResponse.of(user, null);
    }
}
