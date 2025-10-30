package app.usfit.api.user.service.login;

import java.util.Optional;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import app.usfit.api.common.enums.AuthProviderEnum;
import app.usfit.api.security.jwt.JwtTokenProvider;
import app.usfit.api.user.dto.LoginRequest;
import app.usfit.api.user.dto.LoginResponse;
import app.usfit.api.user.entity.User;
import app.usfit.api.user.repository.UserRepository;

@Component
public class LocalLoginHandler implements LoginHandler {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    public LocalLoginHandler(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtTokenProvider jwtTokenProvider) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Override
    public AuthProviderEnum supports() {
        return AuthProviderEnum.LOCAL;
    }

    @Override
    public LoginResponse handle(LoginRequest request) {
        if (request.getEmail() == null || request.getPassword() == null) {
            throw new RuntimeException("email/password 누락");
        }

        // User를 Optional을 통해 안전하게 가져온 후 넣어주기
        Optional<User> userOpt = userRepository.findByEmail(request.getEmail());
        User user = userOpt.orElseThrow(() -> new RuntimeException("존재하지 않는 사용자"));
        
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("비밀번호 불일치");
        }
        // 토큰 발급
        String jwt = jwtTokenProvider.createAccessToken(user.getId(), user.getProvider().name(), user.getEmail());
        return LoginResponse.of(user, jwt);
    }
}
