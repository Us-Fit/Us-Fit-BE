package app.usfit.api.RecruitPlayer.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import app.usfit.api.RecruitPlayer.dto.ApplicationApplyRequest;
import app.usfit.api.RecruitPlayer.dto.ApplicationResponse;
import app.usfit.api.RecruitPlayer.dto.ApplicationStatusRequest;
import app.usfit.api.RecruitPlayer.service.RecruitApplicationService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/recruits/{postId}/applications")
public class RecruitApplicationController {
    private final RecruitApplicationService service;

    public RecruitApplicationController(RecruitApplicationService service) {
        this.service = service;
    }

    //신청
    @PostMapping(consumes= "application/json", produces= "application/json")
    public ResponseEntity<ApplicationResponse> apply(
        @PathVariable("postId") Long postId,
        @RequestBody ApplicationApplyRequest request,
        Authentication authentication
    ) {
        Long userId = Long.parseLong(authentication.getName());
        return ResponseEntity.ok(service.apply(postId, userId, request));
    }

    // 해당 게시글 신청 목록 조회 (작성자)
    @GetMapping(produces = "application/json")
    public ResponseEntity<List<ApplicationResponse>> list(
        @PathVariable("postId") Long postId,
        Authentication authentication
    ) {
        Long userId = Long.parseLong(authentication.getName());
        return ResponseEntity.ok(service.listForPost(postId, userId));
    }

    // 상태 변경(작성자만) - ACCEPTED / REJECTED
    @PatchMapping(value = "/{applicationId}", consumes = "application/json", produces = "application/json")
    public ResponseEntity<ApplicationResponse> changeStatus(
        @PathVariable("postId") Long postId,
        @PathVariable("applicationId") Long applicationId,
        @Valid @RequestBody ApplicationStatusRequest req,
        Authentication authentication
        ) {
        System.out.println("작성자: " + authentication.getName());
        System.out.println("postId: " + postId + ", applicationId: " + applicationId + ", req: " + req);
        Long userId = Long.parseLong(authentication.getName());
        return ResponseEntity.ok(service.changeStatus(postId, applicationId, userId, req));
    }

    @GetMapping(value = "/me", produces = "application/json")
    public ResponseEntity<List<ApplicationResponse>> listMyApplications(Authentication authentication) {
        Long userId = Long.parseLong(authentication.getName());
        return ResponseEntity.ok(service.listMyApplications(userId)); // 내가 신청한 모든 모집글 조회
    }
}
