package app.usfit.api.user.controller;

import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import app.usfit.api.user.dto.ProfileRequest;
import app.usfit.api.user.dto.ProfileResponse;
import app.usfit.api.user.dto.SimpleProfileResponse;
import app.usfit.api.user.service.ProfileService;
import app.usfit.api.user.service.UserProfileImageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;


@Tag(name = "User Profile", description = "사용자 프로필 관련 API")
@RestController
@RequestMapping("/api/user/profile")
public class ProfileController {
    private final ProfileService profileService;
    private final UserProfileImageService userProfileImageService;

    public ProfileController(ProfileService profileService, UserProfileImageService userProfileImageService) {
        this.profileService = profileService;
        this.userProfileImageService = userProfileImageService;
    }

    @Operation(summary = "프로필 업서트")
    @PostMapping
    public ResponseEntity<Void> upsert(Authentication auth, @RequestBody ProfileRequest req) {
        // JWT의 subject가 userId라고 가정. 다르면 Principal에서 ID를 꺼내도록 수정
        Long userId = Long.valueOf(auth.getName());
        profileService.upsert(userId, req);
        return ResponseEntity.noContent().build(); // 204
    }

    @Operation(summary = "프로필 조회 (본인)")
    @GetMapping
    public ResponseEntity<ProfileResponse> get(Authentication auth) {
        Long userId = Long.valueOf(auth.getName());
        return ResponseEntity.ok(profileService.getProfile(userId)); // getProfile로 호출
    }

    @Operation(summary = "프로필 조회 (다른 사용자)", description = "다른 사용자의 프로필을 조회합니다.")
    @GetMapping("/{userId}/profile")
    public ResponseEntity<ProfileResponse> getUserProfile(@PathVariable("userId") Long userId) {
        try {
            ProfileResponse profile = profileService.getProfile(userId);
            return ResponseEntity.ok(profile);
        } catch (IllegalArgumentException e) {
            // 프로필 없음
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.status(500).build();
        }
    }

    // 프로필 이미지 업로드
    @Operation(summary = "프로필 이미지 업로드")
    @PostMapping(value = "profile-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, String>> uploadProfileImage(@RequestPart("file") MultipartFile file, Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            return ResponseEntity.status(401).build();
        }
        Long userId = Long.parseLong(authentication.getName()); // 필요하면 SecurityUtils로 교체
        String key = userProfileImageService.uploadProfileImage(file, userId);
        String url = userProfileImageService.getProfileImageUrl(key);
        return ResponseEntity.ok(Map.of("key", key, "url", url));
    }

    @Operation(summary = "간단한 프로필 조회 (다른 사용자)", description = "특정 사용자의 간단한 프로필을 조회합니다.")
    @GetMapping("/{userId}/simple-profile")
    public ResponseEntity<SimpleProfileResponse> getSimpleProfile(@PathVariable Long userId) {
        try {
            SimpleProfileResponse res = profileService.getSimpleProfile(userId);
            if (res == null) return ResponseEntity.notFound().build();
            return ResponseEntity.ok(res);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.status(500).build();
        }
    }
}
