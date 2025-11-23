package app.usfit.api.club.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
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

    // 동호회원 탈퇴 (소유자/관리자가 강제 탈퇴 처리)
    @DeleteMapping("/{clubId}/members/{memberId}/remove")
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

    // 동호회원 본인 탈퇴
    @DeleteMapping("/{clubId}/members/me/remove")
    public ResponseEntity<Object> leaveClub(@PathVariable Long clubId, Authentication authentication) {
        Long userId = Long.parseLong(authentication.getName());
        try {
            clubinfoService.leaveClub(clubId, userId);
            return ResponseEntity.ok(Map.of("message", "탈퇴 처리되었습니다."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // 소유자가 멤버에게 admin 권한 부여
    @PostMapping("{clubId}/members/{memberId}/grant-admin")
    public ResponseEntity<Object> grantAdmin(@PathVariable("clubId") Long clubId, @PathVariable("memberId") Long memberId, Authentication authentication) {
        Long ownerId = Long.parseLong(authentication.getName());
        try {
            clubinfoService.grantAdminRole(clubId, ownerId, memberId);
            return ResponseEntity.ok(Map.of("message", "관리자 권한이 부여되었습니다."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(403).body(Map.of("error", e.getMessage()));
        }
    }

    // 소유자가 admin 권한 회수
    // 소유자가 멤버의 admin 권한 회수
    @PostMapping("{clubId}/members/{memberId}/revoke-admin")
    public ResponseEntity<Object> revokeAdmin(@PathVariable("clubId") Long clubId, @PathVariable("memberId") Long memberId, Authentication authentication) {
        Long ownerId = Long.parseLong(authentication.getName());
        try {
            clubinfoService.revokeAdminRole(clubId, ownerId, memberId);
            return ResponseEntity.ok(Map.of("message", "관리자 권한이 회수되었습니다."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(403).body(Map.of("error", e.getMessage()));
        }
    }
}
