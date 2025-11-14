package app.usfit.api.club.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import app.usfit.api.club.DTO.ClubMemberResponse;
import app.usfit.api.club.DTO.ClubMemberRoleUpdateRequest;
import app.usfit.api.club.service.ClubinfoService;
import io.swagger.v3.oas.annotations.parameters.RequestBody;

@RestController
@RequestMapping("/api/club-info")
public class ClubInfoController {
    private final ClubinfoService clubinfoService;

    public ClubInfoController(ClubinfoService clubService) {
        this.clubinfoService = clubService;
    }

    // 특정 동호회 동호회원 목록 조회
    @GetMapping("/{clubId}/members")
    public ResponseEntity<List<ClubMemberResponse>> getClubMemebers(@PathVariable("clubId") Long clubId) {
        List<ClubMemberResponse> members = clubinfoService.listClubMemebers(clubId);
        return ResponseEntity.ok(members);
    }


    @PatchMapping("/{clubId}/members/{memberId}/role")
    public ResponseEntity<Object> changeMemberRole(
        @PathVariable("clubId") Long clubId,
        @PathVariable("memberId") Long memberId,
        @RequestBody ClubMemberRoleUpdateRequest req,
        Authentication authentication
    ) {
        if (authentication == null || authentication.getName() == null) {
            return ResponseEntity.status(401).build();
        }
        Long userId = Long.parseLong(authentication.getName());

        var res = clubinfoService.changeMemberRole(clubId, memberId, userId, req.role());
        return ResponseEntity.ok(res);
    }

    // 동호회원 탈퇴
    @DeleteMapping("/{clubId}/members/{memberId}")
        public ResponseEntity<Object> removeMember(
        @PathVariable("clubId") Long clubId,
        @PathVariable("memberId") Long memberId,
        Authentication authentication
    ) {
        if (authentication == null || authentication.getName() == null) {
            return ResponseEntity.status(401).build();
        }

        Long userId = Long.parseLong(authentication.getName());

        try {
            clubinfoService.removeMember(clubId, memberId, userId);
            return ResponseEntity.ok().build();
        } catch (IllegalStateException ex) {
            return ResponseEntity.status(404).body(Map.of("error", ex.getMessage()));
        } catch (SecurityException ex) {
            return ResponseEntity.status(403).body(Map.of("error", ex.getMessage()));
        }
    }
}
