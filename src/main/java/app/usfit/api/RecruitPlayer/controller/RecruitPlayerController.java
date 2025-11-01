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
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/recruits")
public class RecruitPlayerController {

    @Autowired
    private final RecruitPlayerPostService service;

    public RecruitPlayerController(RecruitPlayerPostService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<RecruitPlayerPostResponse> createPost(@Valid @RequestBody RecruitPlayerPostRequest req, Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            return ResponseEntity.status(401).build();
        }

        Long userId = Long.parseLong(authentication.getName());

        RecruitPlayerPost saved = service.createPost(req, userId);

        RecruitPlayerPostResponse res = new RecruitPlayerPostResponse(
            saved.getId(),
            saved.getWriter().getId(),
            saved.getSport().getId(),
            saved.getTitle(),
            saved.getDescription(),
            saved.getLocation(),
            saved.getRecruitDeadline(),
            saved.getActivityStartTime(),
            saved.getActivityDurationMinutes(),
            saved.getMaxMember(),
            saved.getIsActive()
        );

        return ResponseEntity.ok(res);
    }

    @GetMapping
    public ResponseEntity<List<RecruitPlayerPostResponse>> getAllActivePosts() {
        var list = service.getActivePosts().stream().map(p ->
            new RecruitPlayerPostResponse(
                p.getId(),
                p.getWriter().getId(),
                p.getSport().getId(),
                p.getTitle(),
                p.getDescription(),
                p.getLocation(),
                p.getRecruitDeadline(),
                p.getActivityStartTime(),
                p.getActivityDurationMinutes(),
                p.getMaxMember(),
                p.getIsActive()
            )
        ).toList();

        return ResponseEntity.ok(list);
    }

    // 내가 작성한 글 목록
    @GetMapping(value = "/me", produces = "application/json")
    public ResponseEntity<List<RecruitPlayerPostResponse>> getMyPosts(
        Authentication authentication,
        @RequestParam(name = "activeOnly", defaultValue = "true") boolean activeOnly
    ) {
        if (authentication == null || authentication.getName() == null) {
            return ResponseEntity.status(401).build();
        }
        Long userId = Long.parseLong(authentication.getName());

        var list = service.getMyPosts(userId, activeOnly).stream().map(p ->
            new RecruitPlayerPostResponse(
                p.getId(),                          // 엔티티 식별자 getter에 맞춰 사용
                p.getWriter().getId(),
                p.getSport().getId(),
                p.getTitle(),
                p.getDescription(),
                p.getLocation(),
                p.getRecruitDeadline(),
                p.getActivityStartTime(),
                p.getActivityDurationMinutes(),
                p.getMaxMember(),
                p.getIsActive()
            )
        ).toList();

        return ResponseEntity.ok(list);
    }
}