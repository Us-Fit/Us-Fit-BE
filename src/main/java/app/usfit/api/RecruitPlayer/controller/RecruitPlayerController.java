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

import app.usfit.api.RecruitPlayer.dto.ApplicationPostResponse;
import app.usfit.api.RecruitPlayer.dto.RecruitPlayerPostRequest;
import app.usfit.api.RecruitPlayer.dto.RecruitPlayerPostResponse;
import app.usfit.api.RecruitPlayer.dto.RecruitRecommendRequest;
import app.usfit.api.RecruitPlayer.entity.RecruitPlayerPost;
import app.usfit.api.RecruitPlayer.service.RecruitApplicationService;
import app.usfit.api.RecruitPlayer.service.RecruitPlayerPostService;
import app.usfit.api.user.dto.SimpleProfileResponse;
import app.usfit.api.user.service.ProfileService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/recruits")
@io.swagger.v3.oas.annotations.tags.Tag(name = "RecruitPlayer", description = "용병 모집(모집글) 작성 및 조회 API")
public class RecruitPlayerController {

    @Autowired
    private final RecruitPlayerPostService service;

    private final ProfileService profileService;

    private final RecruitApplicationService applicationService;

    public RecruitPlayerController(RecruitPlayerPostService service, ProfileService profileService, RecruitApplicationService applicationService) {
        this.service = service;
        this.profileService = profileService;
        this.applicationService = applicationService;
    }

        @PostMapping
        @io.swagger.v3.oas.annotations.Operation(summary = "모집글 만들기")
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

        RecruitPlayerPostResponse res = service.toResponse(saved, writerProfile);

        return ResponseEntity.ok(res);
    }

    @GetMapping
    @io.swagger.v3.oas.annotations.Operation(summary = "활성 모집글 목록 조회", description = "현재 활성화된 모집글들을 조회합니다. 반환 예시를 참고하세요.")
    public ResponseEntity<List<RecruitPlayerPostResponse>> getAllActivePosts() {
        var list = service.getActivePosts().stream()
            .map(p -> {
                SimpleProfileResponse writerProfile = null;
                try {
                    writerProfile = profileService.getSimpleProfile(p.getWriter().getId());
                } catch (Exception ignored) {}
                return service.toResponse(p, writerProfile);
            })
            .toList();

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
        var list = service.getMyPosts(userId, activeOnly).stream()
            .map(p -> {
                SimpleProfileResponse writerProfile = null;
                try {
                    writerProfile = profileService.getSimpleProfile(p.getWriter().getId());
                } catch (Exception ignored) {}
                return service.toResponse(p, writerProfile);
            })
            .toList();
        return ResponseEntity.ok(list);
    }

    /**
     * 추천 용병 목록 조회
     * - sports: "축구,농구" 처럼 콤마로 구분된 이름(옵션)
     * - sido: 시도 이름(옵션)
     * - sigungu: 시군구 이름(옵션)
     * - limit: 반환 개수 (기본 20)
     */
    @PostMapping("/recommend")
    @io.swagger.v3.oas.annotations.Operation(summary = "용병 추천 시스템", description = "지역 + 선택 운동에 따라 진행")
    public ResponseEntity<List<RecruitPlayerPostResponse>> recommend(@RequestBody RecruitRecommendRequest req) {
        String sido = req.getSidoNm();
        String sigungu = req.getSigunguNm();
        int limit = req.getLimit();
        
        List<String> sports = req != null ? req.getSports() : null;
        List<RecruitPlayerPostResponse> result = service.recommendRecruits(sports, sido, sigungu, limit);
        return ResponseEntity.ok(result);
    }

    @GetMapping(value = "/my/applications", produces = "application/json")
    @Operation(summary = "내가 신청한 내역 조회", description = "로그인한 사용자가 본인이 신청한 모든 모집 내역을 조회합니다. 반환 예시를 참고하세요.")
    public ResponseEntity<List<ApplicationPostResponse>> listMyApplications(Authentication authentication) {
        Long userId = Long.parseLong(authentication.getName());
        return ResponseEntity.ok(applicationService.listMyApplications(userId)); // 내가 신청한 모든 모집글 조회
    }
}