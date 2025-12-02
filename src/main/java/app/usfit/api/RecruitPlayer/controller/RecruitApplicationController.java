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
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/recruits/{postId}/applications")
@io.swagger.v3.oas.annotations.tags.Tag(name = "RecruitApplication", description = "모집 게시글에 대한 신청(용병 모집 신청) API")
public class RecruitApplicationController {
    private final RecruitApplicationService service;

    public RecruitApplicationController(RecruitApplicationService service) {
        this.service = service;
    }

    //신청
    @PostMapping(consumes= "application/json", produces= "application/json")
    @Operation(summary = "모집 글에 신청", description = "해당 모집 게시글에 참여 신청을 합니다. 요청 예시를 참고하세요.")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "신청 요청 예시", content = @io.swagger.v3.oas.annotations.media.Content(mediaType = "application/json", schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = ApplicationApplyRequest.class), examples = {@io.swagger.v3.oas.annotations.media.ExampleObject(name = "한글_예시", value = """
        { "message": "예: 경기를 같이 뛰고 싶습니다. 주말 가능" }
        """)}))
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
    @Operation(summary = "해당 게시글 신청 목록 조회", description = "작성자는 해당 게시글에 들어온 신청 목록을 조회할 수 있습니다. 반환 예시를 참고하세요.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "신청 목록", content = @io.swagger.v3.oas.annotations.media.Content(mediaType = "application/json", examples = {@io.swagger.v3.oas.annotations.media.ExampleObject(name = "한글_예시", value = """
        [ { "id": 1, "postId": 10, "applicantId": 200, "status": "PENDING", "message": "예: 같이 하고 싶습니다.", "appliedAt": "2025-11-20T10:00:00" } ]
        """)}))
    public ResponseEntity<List<ApplicationResponse>> list(
        @PathVariable("postId") Long postId,
        Authentication authentication
    ) {
        Long userId = Long.parseLong(authentication.getName());
        return ResponseEntity.ok(service.listForPost(postId, userId));
    }

    // 상태 변경(작성자만) - ACCEPTED / REJECTED
    @PatchMapping(value = "/{applicationId}", consumes = "application/json", produces = "application/json")
    @Operation(summary = "신청 상태 변경(수락/거절)", description = "작성자가 신청을 수락하거나 거절합니다. 요청 예시를 참고하세요.")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "상태 변경 예시", content = @io.swagger.v3.oas.annotations.media.Content(mediaType = "application/json", schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = ApplicationStatusRequest.class), examples = {@io.swagger.v3.oas.annotations.media.ExampleObject(name = "한글_예시", value = """
        { "status": "ACCEPTED" }
        """)}))
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

    // 특정 모집글에서 참여된 멤버 목록 조회 (작성자 및 참여 멤버)
    @GetMapping(value = "/members", produces = "application/json")
    @Operation(summary = "모집글 참여 멤버 목록 조회", description = "해당 모집글에 참여된 멤버 목록을 조회합니다. 작성자 및 참여 멤버만 조회할 수 있습니다. 반환 예시를 참고하세요.")
    public ResponseEntity<List<ApplicationResponse>> listForPostMember(
        @PathVariable("postId") Long postId,
        Authentication authentication
    ) {
        Long userId = Long.parseLong(authentication.getName());
        return ResponseEntity.ok(service.listForPostMember(postId, userId));    
    }

    @PostMapping(value = "/{applicationId}/selfCancel", produces = "application/json")
    @Operation(summary = "신청자 본인 신청 취소", description = "신청자가 본인의 신청을 취소합니다. 기록 보존을 위해 상태를 CANCELED로 변경합니다.")
    public ResponseEntity<ApplicationResponse> selfCancelApplication(
        @PathVariable("postId") Long postId,
        @PathVariable("applicationId") Long applicationId,
        Authentication authentication
    ) {
        Long userId = Long.parseLong(authentication.getName());
        return ResponseEntity.ok(service.exitRecruit(postId, applicationId, userId));
    }
}
