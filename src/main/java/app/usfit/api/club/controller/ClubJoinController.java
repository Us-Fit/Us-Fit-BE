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
public class ClubJoinController {
    private final ClubJoinService clubJoinService;

    public ClubJoinController(ClubJoinService clubService) {
        this.clubJoinService = clubService;
    }

    @PostMapping("/{clubId}/requests")
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
