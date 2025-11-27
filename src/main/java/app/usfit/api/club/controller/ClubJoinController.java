package app.usfit.api.club.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import app.usfit.api.club.DTO.ClubJoinDecisionRequest;
import app.usfit.api.club.DTO.ClubJoinRequest;
import app.usfit.api.club.DTO.ClubJoinResponse;
import app.usfit.api.club.service.ClubJoinService;

@RestController
@RequestMapping("/api/club/join")
@io.swagger.v3.oas.annotations.tags.Tag(name = "ClubJoin", description = "동호회 가입 신청 관련 API")
public class ClubJoinController {
    private final ClubJoinService clubJoinService;

    public ClubJoinController(ClubJoinService clubService) {
        this.clubJoinService = clubService;
    }

        @PostMapping("/{clubId}/requests")
        @io.swagger.v3.oas.annotations.Operation(summary = "가입 신청 생성", description = "동호회에 가입 신청을 생성합니다. 요청 예시를 참고하세요.")
        @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "가입 신청 예시", content = @io.swagger.v3.oas.annotations.media.Content(mediaType = "application/json", examples = {@io.swagger.v3.oas.annotations.media.ExampleObject(name = "한글_예시", value = """
            { "message": "예: 함께 활동하고 싶어요. 주말에 참여 가능합니다." }
            """)}))
        public ResponseEntity<Object> requestJoin(@PathVariable("clubId") Long clubId, @RequestBody ClubJoinRequest req, Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            return ResponseEntity.status(401).build();
        }
        Long userId = Long.parseLong(authentication.getName());

        try {
            ClubJoinResponse jr = clubJoinService.createJoinRequest(clubId, userId, req.getMessage());
            return ResponseEntity.ok(jr);
        } catch (IllegalStateException ex) {
            return ResponseEntity.status(409).body(Map.of("error", ex.getMessage()));
        }
    }

    // 동호회장이 전체 신청자 조회
        @GetMapping("/{clubId}/requests")
        @io.swagger.v3.oas.annotations.Operation(summary = "가입 신청 목록 조회", description = "동호회장이 해당 동호회의 전체 가입 신청 목록을 조회합니다. 반환 예시를 참고하세요.")
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "신청 목록", content = @io.swagger.v3.oas.annotations.media.Content(mediaType = "application/json", examples = {@io.swagger.v3.oas.annotations.media.ExampleObject(name = "한글_예시", 
        value = """
            [
                {
                    "id": 1,
                    "clubId": 10,
                    "user": {
                    "userId": 12,
                    "nickname": "이름",
                    "profileImageUrl": null,
                    "gender": true
                    },
                    "status": "pending",
                    "message": "예: 같이 하면 좋겠습니다.",
                    "requestedAt": "2025-11-20T10:00:00"
                }
            ]
            """)
            }))
        public ResponseEntity<Object> listRequests(@PathVariable("clubId") Long clubId, Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            return ResponseEntity.status(401).build();
        }
        Long userId = Long.parseLong(authentication.getName());

        try {
            List<ClubJoinResponse> list = clubJoinService.listRequestsForClub(clubId, userId);
            return ResponseEntity.ok(list);
        } catch (IllegalStateException ex) {
            return ResponseEntity.status(404).body(Map.of("error", ex.getMessage()));
        } catch (SecurityException ex) {
            return ResponseEntity.status(403).body(Map.of("error", ex.getMessage()));
        }
    }

    // 추가: 요청 id로 수락/거절 결정
        @PostMapping("/requests/{requestId}/decision")
        @io.swagger.v3.oas.annotations.Operation(summary = "가입 신청 수락/거절", description = "작성자는 신청을 수락하거나 거절할 수 있습니다. 요청 예시를 참고하세요.")
        @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "수락 여부 예시", content = @io.swagger.v3.oas.annotations.media.Content(mediaType = "application/json", examples = {@io.swagger.v3.oas.annotations.media.ExampleObject(name = "한글_예시", value = """
            { "accept": true }
            """)}))
        public ResponseEntity<Object> decideRequest(@PathVariable("requestId") Long requestId,
                            @RequestBody ClubJoinDecisionRequest decision,
                            Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            return ResponseEntity.status(401).build();
        }
        Long userId = Long.parseLong(authentication.getName());

        try {
            ClubJoinResponse res = clubJoinService.decideRequest(requestId, userId, decision.isAccept());
            return ResponseEntity.ok(res);
        } catch (IllegalStateException ex) {
            return ResponseEntity.status(404).body(Map.of("error", ex.getMessage()));
        } catch (SecurityException ex) {
            return ResponseEntity.status(403).body(Map.of("error", ex.getMessage()));
        }
    }
}
