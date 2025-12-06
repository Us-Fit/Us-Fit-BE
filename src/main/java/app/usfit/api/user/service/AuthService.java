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
import jakarta.transaction.Transactional;

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
                       List<LoginHandler> handlers
                       ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        // stream -> 데이터 요소들의 파이프라인 처리용 시퀀스
        this.handlerMap = handlers.stream().collect(Collectors.toMap(h -> h.supports(), h -> h));
    }

    // 회원가입
    @Transactional
    public User register(UserRegisterRequest req) {
        // 1) User 생성
        User user = createUser(req);
        return user;
    }

    private User createUser(UserRegisterRequest req) {
        userRepository.findByEmail(req.getEmail()).ifPresent(u -> {
            throw new IllegalArgumentException("이미 존재하는 이메일");
        });
        User user = User.builder()
            .email(req.getEmail())
            .password(passwordEncoder.encode(req.getPassword()))
            .provider(AuthProviderEnum.LOCAL)
            .build();
        return userRepository.save(user);
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

    //계정 삭제
    @Transactional
    public void deleteUser(Long userId) {
        var user = userRepository.findById(userId)
            .orElseThrow(() -> 
                new jakarta.persistence.EntityNotFoundException("사용자를 찾을 수 없습니다: " + userId)
            );
    
        userRepository.deleteById(userId);
    }
}