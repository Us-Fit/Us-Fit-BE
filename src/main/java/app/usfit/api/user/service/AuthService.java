package app.usfit.api.user.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import app.usfit.api.user.dto.UserRegisterRequest;
import app.usfit.api.user.entity.User;
import app.usfit.api.user.repository.UserRepository;

///<summary>
/// 회원가입 관련 기능들을 담당하는 서비스
/// </summary>

@Service
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
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
                .build();
                
        return userRepository.save(newUser);
    }
    
    // DTO를 받는 편의 메서드 (내부적으로 위 메서드 호출)
    public User register(UserRegisterRequest request) {
        return register(request.getEmail(), request.getNickname(), request.getPassword());
    }
}