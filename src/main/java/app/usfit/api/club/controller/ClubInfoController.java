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

import app.usfit.api.club.DTO.ClubDetailInfoResponse;
import app.usfit.api.club.DTO.ClubMemberResponse;
import app.usfit.api.club.DTO.ClubMemberRoleUpdateRequest;
import app.usfit.api.club.service.ClubinfoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.parameters.RequestBody;

@RestController
@RequestMapping("/api/club-info")
@io.swagger.v3.oas.annotations.tags.Tag(name = "ClubInfo", description = "동호회 멤버 관리 관련 API (탈퇴, 권한 부여 등). 멤버 역할: owner, admin, member")
public class ClubInfoController {
    private final ClubinfoService clubinfoService;

    public ClubInfoController(ClubinfoService clubService) {
        this.clubinfoService = clubService;
    }
    // 특정 동호회 상세 정보 조회
    @GetMapping("/{clubId}")
    @Operation(summary = "동호회 상세 정보 조회", description = "특정 동호회의 상세 정보를 반환합니다. 반환 예시는 아래를 참고하세요.")
    public ResponseEntity<Object> getClubDetail(@PathVariable("clubId") Long clubId) {
        try {
            ClubDetailInfoResponse resp = clubinfoService.getClubDetail(clubId);
            return ResponseEntity.ok(resp);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "서버 오류"));
        }
    }

    // 특정 동호회 동호회원 목록 조회
    @GetMapping("/{clubId}/members")
    @Operation(summary = "동호회 멤버 목록 조회", description = "해당 동호회의 멤버 목록을 반환합니다. 반환 예시는 아래를 참고하세요. `role` 필드는 'owner', 'admin', 'member' 중 하나입니다.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "멤버 목록", content = @io.swagger.v3.oas.annotations.media.Content(mediaType = "application/json", examples = {@io.swagger.v3.oas.annotations.media.ExampleObject(name = "한글_예시", value = """
            [
              { "id": 1, "clubId": 10, "userId": 100, "role": "owner", "status": "active", "joinedAt": "2025-11-01T12:34:56" },
              { "id": 2, "clubId": 10, "userId": 101, "role": "admin", "status": "active", "joinedAt": "2025-11-05T09:00:00" }
            ]
            """)}))
    public ResponseEntity<List<ClubMemberResponse>> getClubMemebers(@PathVariable("clubId") Long clubId) {
        List<ClubMemberResponse> members = clubinfoService.listClubMemebers(clubId);
        return ResponseEntity.ok(members);
    }


        @PatchMapping("/{clubId}/members/{memberId}/role")
        @io.swagger.v3.oas.annotations.Operation(summary = "멤버 역할 변경", description = "지정된 멤버의 역할을 변경합니다. 요청 바디 예시를 참고하세요. 허용되는 역할 값: 'owner', 'admin', 'member'.")
        @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "변경할 역할 예시 (role에 'owner'|'admin'|'member' 입력)", content = @io.swagger.v3.oas.annotations.media.Content(mediaType = "application/json", examples = {@io.swagger.v3.oas.annotations.media.ExampleObject(name = "한글_예시", value = """
            { "role": "admin" }
            """)}))
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
    @io.swagger.v3.oas.annotations.Operation(summary = "멤버 강제 제거", description = "소유자 또는 관리자가 특정 멤버를 강제로 탈퇴 처리합니다. -> 소유자는 관리자, 멤버 다 제거 가능 / 관리자는 멤버만 제거 가능")
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
    @io.swagger.v3.oas.annotations.Operation(summary = "본인 탈퇴", description = "로그인된 사용자가 스스로 동호회를 탈퇴합니다.")
    public ResponseEntity<Object> leaveClub(
        @io.swagger.v3.oas.annotations.Parameter(in = io.swagger.v3.oas.annotations.enums.ParameterIn.PATH, name = "clubId", required = true, description = "탈퇴할 동호회 ID", example = "123")
        @PathVariable("clubId") Long clubId, Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            return ResponseEntity.status(401).body(Map.of("error", "인증 필요"));
        }
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
    @io.swagger.v3.oas.annotations.Operation(summary = "관리자 권한 부여", description = "소유자가 특정 멤버에게 관리자 권한을 부여합니다.")
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
    @io.swagger.v3.oas.annotations.Operation(summary = "관리자 권한 회수", description = "소유자가 특정 멤버의 관리자 권한을 회수합니다.")
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
