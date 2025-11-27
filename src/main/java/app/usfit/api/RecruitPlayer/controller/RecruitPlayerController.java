package app.usfit.api.RecruitPlayer.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import app.usfit.api.RecruitPlayer.dto.RecruitPlayerPostRequest;
import app.usfit.api.RecruitPlayer.dto.RecruitPlayerPostResponse;
import app.usfit.api.RecruitPlayer.entity.RecruitPlayerPost;
import app.usfit.api.RecruitPlayer.service.RecruitPlayerPostService;
import app.usfit.api.user.dto.SimpleProfileResponse;
import app.usfit.api.user.service.ProfileService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/recruits")
@io.swagger.v3.oas.annotations.tags.Tag(name = "RecruitPlayer", description = "용병 모집(모집글) 작성 및 조회 API")
public class RecruitPlayerController {

    @Autowired
    private final RecruitPlayerPostService service;

    private final ProfileService profileService;

    public RecruitPlayerController(RecruitPlayerPostService service, ProfileService profileService) {
        this.service = service;
        this.profileService = profileService;
    }

        @PostMapping
        @io.swagger.v3.oas.annotations.Operation(summary = "모집글 작성", description = "새 모집글을 작성합니다. 요청 예시를 참고하세요.")
        @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "모집글 작성 예시", content = @io.swagger.v3.oas.annotations.media.Content(mediaType = "application/json", schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = RecruitPlayerPostRequest.class), examples = {@io.swagger.v3.oas.annotations.media.ExampleObject(name = "예시",
        value = """
                {
                    "sportName": "축구 (예: 운동 이름)",
                    "title": "주말 축구 용병 모집 (예: 모임 제목)",
                    "description": "주말에 같이 뛸 분 구합니다. 실력 상관없음. (예: 상세 설명)",
                    "facilityId": 5,
                    "recruitDeadline": "2025-12-01T18:00:00 (ISO-8601 날짜/시간)",
                    "activityStartTime": "2025-12-07T10:00:00 (ISO-8601 날짜/시간)",
                    "activityDurationMinutes": 90,
                    "maxMember": 5
                }
                """)}))
        public ResponseEntity<RecruitPlayerPostResponse> createPost(@Valid @RequestBody RecruitPlayerPostRequest req, Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            return ResponseEntity.status(401).build();
        }

        Long userId = Long.parseLong(authentication.getName());

        RecruitPlayerPost saved = service.createPost(req, userId);

        SimpleProfileResponse writerProfile = null;
        try {
            writerProfile = profileService.getSimpleProfile(saved.getWriter().getId());
        } catch (Exception ignored) {}

        RecruitPlayerPostResponse res = new RecruitPlayerPostResponse(
            saved.getId(),
            writerProfile,
            saved.getSport().getId(),
            saved.getSport().getName(),
            saved.getTitle(),
            saved.getDescription(),
            saved.getRecruitDeadline(),
            saved.getActivityStartTime(),
            saved.getActivityDurationMinutes(),
            saved.getMaxMember(),
            saved.getIsActive(),
            saved.getFacilityId()
        );

        return ResponseEntity.ok(res);
    }

    @GetMapping
    @io.swagger.v3.oas.annotations.Operation(summary = "활성 모집글 목록 조회", description = "현재 활성화된 모집글들을 조회합니다. 반환 예시를 참고하세요.")
    public ResponseEntity<List<RecruitPlayerPostResponse>> getAllActivePosts() {
        var list = service.getActivePosts().stream().map(p -> {
            SimpleProfileResponse writerProfile = null;
            try {
                writerProfile = profileService.getSimpleProfile(p.getWriter().getId());
            } catch (Exception ignored) {}
            return new RecruitPlayerPostResponse(
                p.getId(),
                writerProfile,
                p.getSport().getId(),
                p.getSport().getName(),
                p.getTitle(),
                p.getDescription(),
                p.getRecruitDeadline(),
                p.getActivityStartTime(),
                p.getActivityDurationMinutes(),
                p.getMaxMember(),
                p.getIsActive(),
                p.getFacilityId()
            );
         }).toList();

        return ResponseEntity.ok(list);
    }

    // 내가 작성한 글 목록
    @GetMapping(value = "/me", produces = "application/json")
    @io.swagger.v3.oas.annotations.Operation(summary = "내가 작성한 모집글 조회", description = "로그인한 사용자가 작성한 모집글 목록을 조회합니다. 쿼리 파라미터 `activeOnly`로 활성글만 필터링 가능합니다.")
    public ResponseEntity<List<RecruitPlayerPostResponse>> getMyPosts(
        Authentication authentication,
        @RequestParam(name = "activeOnly", defaultValue = "true") boolean activeOnly
    ) {
        if (authentication == null || authentication.getName() == null) {
            return ResponseEntity.status(401).build();
        }
        Long userId = Long.parseLong(authentication.getName());
        var list = service.getMyPosts(userId, activeOnly).stream().map(p -> {
            SimpleProfileResponse writerProfile = null;
                try {
                    writerProfile = profileService.getSimpleProfile(p.getWriter().getId());
                } catch (Exception ignored) {}
                return new RecruitPlayerPostResponse(
                    p.getId(),                          // 엔티티 식별자 getter에 맞춰 사용
                    writerProfile,
                    p.getSport().getId(),
                    p.getSport().getName(),
                    p.getTitle(),
                    p.getDescription(),
                    p.getRecruitDeadline(),
                    p.getActivityStartTime(),
                    p.getActivityDurationMinutes(),
                    p.getMaxMember(),
                    p.getIsActive(),
                    p.getFacilityId()
                );
            }).toList();
        return ResponseEntity.ok(list);
    }
}