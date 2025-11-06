package app.usfit.api.user.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import app.usfit.api.user.dto.ProfileRequest;
import app.usfit.api.user.dto.ProfileResponse;
import app.usfit.api.user.service.ProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;


@Tag(name = "User Profile", description = "사용자 프로필 관련 API")
@RestController
@RequestMapping("/api/user/profile")
public class ProfileController {
    private final ProfileService profileService;
    public ProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    @Operation(summary = "프로필 업서트")
    @PostMapping
    public ResponseEntity<Void> upsert(Authentication auth, @RequestBody ProfileRequest req) {
        // JWT의 subject가 userId라고 가정. 다르면 Principal에서 ID를 꺼내도록 수정
        Long userId = Long.valueOf(auth.getName());
        profileService.upsert(userId, req);
        return ResponseEntity.noContent().build(); // 204
    }

    @Operation(summary = "프로필 조회")
    @GetMapping
    public ResponseEntity<ProfileResponse> get(Authentication auth) {
        Long userId = Long.valueOf(auth.getName());
        return ResponseEntity.ok(profileService.getProfile(userId)); // getProfile로 호출
    }
    
    
}
