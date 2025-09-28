package app.usfit.api.user.controller;

import java.util.List;
import java.util.Optional;

import app.usfit.api.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import app.usfit.api.user.entity.User;
import app.usfit.api.user.service.*;
import app.usfit.api.user.dto.UserRegisterRequest;

@RestController
@RequestMapping("/api/user")
public class UserController {
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository repo;
    
    @PostMapping("/register")
    public ResponseEntity<User> register(@RequestBody UserRegisterRequest request) {
        try {
            User registeredUser = authService.register(request);
            return ResponseEntity.ok(registeredUser);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(null);
        }
    }
    
    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestBody LoginRequest loginRequest) {
        Optional<User> user = userService.login(loginRequest.getUsername(), loginRequest.getPassword());
        if (user.isPresent()) {
            return ResponseEntity.ok("Login successful");
        } else {
            return ResponseEntity.badRequest().body("Invalid credentials");
        }
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

    // 로그인 요청을 위한 내부 클래스
    public static class LoginRequest {
        private String username;
        private String password;
        
        // getters and setters
        public String getUsername() {
            return username;
        }
        
        public void setUsername(String username) {
            this.username = username;
        }
        
        public String getPassword() {
            return password;
        }
        
        public void setPassword(String password) {
            this.password = password;
        }
    }
}