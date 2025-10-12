package app.usfit.api.user.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import app.usfit.api.user.dto.LoginRequest;
import app.usfit.api.user.dto.LoginResponse;
import app.usfit.api.user.dto.UserRegisterRequest;
import app.usfit.api.user.entity.User;
import app.usfit.api.user.repository.UserRepository;
import app.usfit.api.user.service.AuthService;
import app.usfit.api.user.service.UserService;
@RestController
@RequestMapping("/api/user")
public class UserController {
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository repo;
    
    @PostMapping("/login")
    public LoginResponse login(@RequestBody LoginRequest request) {
        return authService.login(request);

        // 프론트에서 {provider: 'KAKAO', accessToken: ... }형태로 받으면 됨.
    }

    //mock data 잘 들어갔는지 확인 - users list 확인
    @GetMapping
    public List<User> all(){
        return repo.findAll();
    }
    
    // User 테이블의 데이터 개수 확인
    @GetMapping("/count")
    public ResponseEntity<Long> getUserCount() {
        long count = userService.countUsers();
        return ResponseEntity.ok(count);
    }
    
    // 회원가입 엔드포인트 (이메일, 이름, 비밀번호)
    @PostMapping("/signup")
    public ResponseEntity<String> signup(@RequestBody UserRegisterRequest request) {
        try {
            User newUser = authService.register(request);
            return ResponseEntity.ok("회원가입 성공! 사용자 ID: " + newUser.getId());
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body("회원가입 실패: " + e.getMessage());
        }
    }
}