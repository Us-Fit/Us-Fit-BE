package app.usfit.api.user.service;

import java.util.List;
import java.util.Optional;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import app.usfit.api.user.entity.User;
import app.usfit.api.user.repository.UserRepository;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }
    
    public Optional<User> login(String email, String rawPassword) {
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isPresent() && passwordEncoder.matches(rawPassword, userOpt.get().getPassword())) {
            return userOpt;
        }
        return Optional.empty();
    }
    
    // 모든 사용자 조회 메서드 추가
    public List<User> findAll() {
        return userRepository.findAll();
    }
    
    // 사용자 수 조회 메서드 추가
    public long countUsers() {
        return userRepository.count();
    }

    // ID로 사용자 조회 메서드 추가
    public Optional<User> findbyId(Long id) {
        return userRepository.findById(id);
    }
}
