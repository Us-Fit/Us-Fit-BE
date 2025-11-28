package app.usfit.api.club.activity.controller;

import app.usfit.api.club.activity.dto.ActivityCreateRequest;
import app.usfit.api.club.activity.dto.ActivityMemberResponse;
import app.usfit.api.club.activity.dto.ActivityResponse;
import app.usfit.api.club.activity.dto.ActivityUpdateRequest;
import app.usfit.api.club.activity.entity.ActivityMemberStatus;
import app.usfit.api.club.activity.service.ActivityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/clubs/{clubId}/activities")
@Tag(name = "Club Activity", description = "동호회 활동(모임) 관리 API")
public class ActivityController {

    private final ActivityService activityService;

    @Operation(
            summary = "클럽 활동 생성",
            description = "특정 클럽에 새로운 활동(모임)을 생성합니다."
    )
    @PostMapping
    public ActivityResponse createActivity(
            @Parameter(description = "클럽 ID", example = "10")
            @PathVariable Long clubId,
            @RequestBody ActivityCreateRequest request
    ) {
        return activityService.createActivity(clubId, request);
    }

    @Operation(
            summary = "클럽 활동 목록 조회",
            description = "해당 클럽에서 생성된 모든 활동 목록을 조회합니다."
    )
    @GetMapping
    public List<ActivityResponse> getActivitiesByClub(
            @Parameter(description = "클럽 ID", example = "10")
            @PathVariable Long clubId
    ) {
        return activityService.getActivitiesByClub(clubId);
    }

    @Operation(
            summary = "클럽 단일 활동 조회",
            description = "특정 활동 ID에 해당하는 활동 상세 정보를 조회합니다."
    )
    @GetMapping("/{activityId}")
    public ActivityResponse getActivity(
            @Parameter(description = "클럽 ID", example = "10")
            @PathVariable Long clubId,
            @Parameter(description = "활동 ID", example = "1")
            @PathVariable Long activityId
    ) {
        // clubId 검증 로직 필요하면 service에 추가해서 사용
        return activityService.getActivity(activityId);
    }

    @Operation(
            summary = "클럽 활동 수정",
            description = "특정 활동의 기본 정보 및 상태를 수정합니다."
    )
    @PutMapping("/{activityId}")
    public ActivityResponse updateActivity(
            @Parameter(description = "클럽 ID", example = "10")
            @PathVariable Long clubId,
            @Parameter(description = "활동 ID", example = "1")
            @PathVariable Long activityId,
            @RequestBody ActivityUpdateRequest request
    ) {
        return activityService.updateActivity(activityId, request);
    }

    @Operation(
            summary = "클럽 활동 삭제",
            description = "특정 활동을 삭제합니다."
    )
    @DeleteMapping("/{activityId}")
    public void deleteActivity(
            @Parameter(description = "클럽 ID", example = "10")
            @PathVariable Long clubId,
            @Parameter(description = "활동 ID", example = "1")
            @PathVariable Long activityId
    ) {
        activityService.deleteActivity(activityId);
    }

    @Operation(summary = "활동 참여 신청", description = "해당 활동에 참여합니다. (클럽 멤버만 가능)")
    @PostMapping("/{activityId}/join")
    public ActivityMemberResponse joinActivity(
            @PathVariable Long clubId,
            @PathVariable Long activityId
    ) {
        return activityService.joinActivity(clubId, activityId);
    }

    @Operation(summary = "활동 나가기", description = "본인이 참여 중인 활동에서 나갑니다.")
    @PostMapping("/{activityId}/leave")
    public void leaveActivity(
            @PathVariable Long clubId,
            @PathVariable Long activityId
    ) {
        activityService.leaveActivity(activityId);
    }

    @Operation(summary = "활동 멤버 리스트 조회", description = "해당 활동의 참여자 목록을 조회합니다. (owner/admin만)")
    @GetMapping("/{activityId}/members")
    public List<ActivityMemberResponse> getActivityMembers(
            @PathVariable Long clubId,
            @PathVariable Long activityId
    ) {
        return activityService.getActivityMembers(clubId, activityId);
    }

    @Operation(summary = "활동 멤버 상태 변경", description = "활동 멤버를 강퇴하거나 상태를 변경합니다. (owner/admin만)")
    @PatchMapping("/members/{activityMemberId}/status")
    public ActivityMemberResponse updateActivityMemberStatus(
            @PathVariable Long clubId,
            @PathVariable Long activityMemberId,
            @RequestParam ActivityMemberStatus status
    ) {
        return activityService.updateActivityMemberStatus(clubId, activityMemberId, status);
    }
}
