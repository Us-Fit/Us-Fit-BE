package app.usfit.api.user.service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import app.usfit.api.common.enums.AuthProviderEnum;
import app.usfit.api.user.dto.LoginRequest;
import app.usfit.api.user.dto.LoginResponse;
import app.usfit.api.user.dto.UserRegisterRequest;
import app.usfit.api.user.entity.User;
import app.usfit.api.user.repository.UserRepository;
import app.usfit.api.user.service.login.LoginHandler;

///<summary>
/// 회원가입 관련 기능들을 담당하는 서비스
/// </summary>

@Service
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final Map<AuthProviderEnum, LoginHandler> handlerMap;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       List<LoginHandler> handlers) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        // stream -> 데이터 요소들의 파이프라인 처리용 시퀀스
        this.handlerMap = handlers.stream().collect(Collectors.toMap(h -> h.supports(), h -> h));
    }

    // 회원가입
    public User register(String email, String nickname, String password) {
        // 이메일 중복 확인
        if (userRepository.findByEmail(email).isPresent()) {
            throw new RuntimeException("이미 존재하는 이메일입니다: " + email);
        }
        
        // 새 사용자 생성
        User newUser = User.builder()
                .email(email)
                .nickname(nickname)
                .username(email)  // username을 email로 설정
                .password(passwordEncoder.encode(password))
                .provider(AuthProviderEnum.LOCAL)
                .build();
        
        return userRepository.save(newUser);
    }
    
    // DTO를 받는 편의 메서드 (내부적으로 위 메서드 호출)
    public User register(UserRegisterRequest request) {
        return register(request.getEmail(), request.getNickname(), request.getPassword());
    }

    // 전략 패턴 기반 로그인 진입점
    public LoginResponse login(LoginRequest request) {
        if (request.getProvider() == null) {
            throw new RuntimeException("provider 누락");
        }
        var handler = handlerMap.get(request.getProvider());
        if (handler == null) {
            throw new RuntimeException("지원하지 않는 provider: " + request.getProvider());
        }
        return handler.handle(request);
    }
}