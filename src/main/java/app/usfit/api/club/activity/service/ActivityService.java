package app.usfit.api.club.activity.service;

import app.usfit.api.club.activity.dto.ActivityCreateRequest;
import app.usfit.api.club.activity.dto.ActivityMemberResponse;
import app.usfit.api.club.activity.dto.ActivityResponse;
import app.usfit.api.club.activity.dto.ActivityUpdateRequest;
import app.usfit.api.club.activity.entity.Activity;
import app.usfit.api.club.activity.entity.ActivityMember;
import app.usfit.api.club.activity.entity.ActivityMemberStatus;
import app.usfit.api.club.activity.entity.ActivityStatus;
import app.usfit.api.club.activity.repository.ActivityMemberRepository;
import app.usfit.api.club.activity.repository.ActivityRepository;
import app.usfit.api.club.entity.Club;
import app.usfit.api.club.entity.ClubMember;
import app.usfit.api.club.repository.ClubMemberRepository;
import app.usfit.api.club.repository.ClubRepository;
import app.usfit.api.facility.entity.Facility;
import app.usfit.api.facility.repository.FacilityRepository;
import app.usfit.api.security.SecurityUtil;
import app.usfit.api.user.entity.User;
import app.usfit.api.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ActivityService {

    private final ActivityRepository activityRepository;
    private final ActivityMemberRepository activityMemberRepository;
    private final ClubRepository clubRepository;
    private final FacilityRepository facilityRepository;
    private final UserRepository userRepository;
    private final ClubMemberRepository clubMemberRepository;

    // ──────────────────────────
    // 활동 생성 (owner/admin만)
    // ──────────────────────────
    @Transactional
    public ActivityResponse createActivity(Long clubId, ActivityCreateRequest request) {
        Long currentUserId = SecurityUtil.getCurrentUserId();

        Club club = clubRepository.findById(clubId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 클럽입니다."));

        validateClubManager(club.getId(), currentUserId); // 권한 체크

        Facility facility = null;
        if (request.getActFacilityId() != null) {
            facility = facilityRepository.findById(request.getActFacilityId())
                    .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 시설입니다."));
        }

        Activity activity = Activity.builder()
                .club(club)
                .title(request.getTitle())
                .description(request.getDescription())
                .activityDate(request.getActivityDate())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .maxPeople(request.getMaxPeople())
                .currentPeople(0)
                .actFacility(facility)
                .locationName(request.getLocationName())
                .status(ActivityStatus.OPEN)
                .build();

        Activity saved = activityRepository.save(activity);
        return ActivityResponse.from(saved);
    }

    //get 활동
    public ActivityResponse getActivity(Long activityId) {
        Activity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 활동입니다."));
        return ActivityResponse.from(activity);
    }

    //club의 활동들 전체 받아오기
    public List<ActivityResponse> getActivitiesByClub(Long clubId) {
        return activityRepository.findByClubId(clubId).stream()
                .map(ActivityResponse::from)
                .toList();
    }

    //활동 수정 - owner admin만
    @Transactional
    public ActivityResponse updateActivity(Long activityId, ActivityUpdateRequest request) {
        Long currentUserId = SecurityUtil.getCurrentUserId();

        Activity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 활동입니다."));

        Facility facility = null;
        if (request.getActFacilityId() != null) {
            facility = facilityRepository.findById(request.getActFacilityId())
                    .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 시설입니다."));
        }
        Long clubId = activity.getClub().getId();
        validateClubManager(clubId, currentUserId);

        activity.updateBasicInfo(
                request.getTitle(),
                request.getDescription(),
                request.getActivityDate(),
                request.getStartTime(),
                request.getEndTime(),
                request.getMaxPeople(),
                request.getLocationName()
        );

        if (request.getStatus() != null) {
            activity.changeStatus(request.getStatus());
        }

        if (facility != null) {
            // null 허용: 시설 변경 / 제거 로직은 필요 시 따로 메서드 만들어도 됨
            activity = Activity.builder()
                    .id(activity.getId())
                    .club(activity.getClub())
                    .title(activity.getTitle())
                    .description(activity.getDescription())
                    .activityDate(activity.getActivityDate())
                    .startTime(activity.getStartTime())
                    .endTime(activity.getEndTime())
                    .maxPeople(activity.getMaxPeople())
                    .currentPeople(activity.getCurrentPeople())
                    .actFacility(facility)
                    .locationName(activity.getLocationName())
                    .status(activity.getStatus())
                    .build();
            activityRepository.save(activity);
        }

        return ActivityResponse.from(activity);
    }

    //활동 삭제 - owner, admin만
    @Transactional
    public void deleteActivity(Long activityId) {
        Long currentUserId = SecurityUtil.getCurrentUserId();

        Activity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 활동입니다."));

        Long clubId = activity.getClub().getId();
        validateClubManager(clubId, currentUserId);

        activityRepository.delete(activity);
    }

    // ──────────────────────────
    // 활동 참여 (일반 멤버도 가능)
    // ──────────────────────────
    @Transactional
    public ActivityMemberResponse joinActivity(Long clubId, Long activityId) {
        Long currentUserId = SecurityUtil.getCurrentUserId();

        Activity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 활동입니다."));

        if (activity.isExpired()) {
            activity.changeStatus(ActivityStatus.CLOSED); // 상태도 닫아버리고
            throw new IllegalStateException("이미 종료된 활동입니다.");
        }

        // 클럽 일치 여부 체크 (URL clubId랑 activity.club.id가 다르면 잘못된 요청)
        if (!activity.getClub().getId().equals(clubId)) {
            throw new IllegalArgumentException("클럽과 활동 정보가 일치하지 않습니다.");
        }

        // 클럽 멤버인지 확인 (status 등은 네가 정의한 값에 맞게)
        ClubMember clubMember = clubMemberRepository
                .findByClubIdAndUserId(clubId, currentUserId)
                .orElseThrow(() -> new AccessDeniedException("이 클럽의 멤버만 활동에 참여할 수 있습니다."));

        // 이미 JOINED 상태인지 확인
        if (activityMemberRepository.existsByActivityIdAndUserIdAndStatus(
                activityId, currentUserId, ActivityMemberStatus.JOINED)) {
            throw new IllegalArgumentException("이미 참여 중인 활동입니다.");
        }

        // 정원 체크
        if (activity.getCurrentPeople() >= activity.getMaxPeople()) {
            throw new IllegalStateException("모집 인원이 가득 찼습니다.");
        }

        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));

        ActivityMember activityMember = ActivityMember.builder()
                .activity(activity)
                .user(user)
                .status(ActivityMemberStatus.JOINED)
                .joinedAt(LocalDateTime.now())
                .build();

        activityMemberRepository.save(activityMember);

        // 활동 참여 인원 +1
        activity.increaseCurrentPeople(); // 엔티티에 편의 메서드 하나 만들어줘

        return ActivityMemberResponse.from(activityMember);
    }

    // ──────────────────────────
    // 활동 나가기 (본인만)
    // ──────────────────────────
    @Transactional
    public void leaveActivity(Long activityId) {
        Long currentUserId = SecurityUtil.getCurrentUserId();

        ActivityMember am = activityMemberRepository
                .findByActivityIdAndUserId(activityId, currentUserId)
                .orElseThrow(() -> new IllegalArgumentException("참여 정보가 없습니다."));

        if (am.getStatus() != ActivityMemberStatus.JOINED) {
            throw new IllegalStateException("이미 참여 중이 아닌 상태입니다.");
        }

        am.changeStatus(ActivityMemberStatus.CANCELED);
        am.getActivity().decreaseCurrentPeople();
    }

    // ──────────────────────────
    // 활동 멤버 리스트 조회 (일반 멤버도 가능)
    // ──────────────────────────
    public List<ActivityMemberResponse> getActivityMembers(Long clubId, Long activityId) {

        Activity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 활동입니다."));

        return activityMemberRepository
                .findByActivityIdAndStatus(activityId, ActivityMemberStatus.JOINED)
                .stream()
                .map(ActivityMemberResponse::from)
                .toList();
    }

    // ──────────────────────────
    // 활동 멤버 상태 변경 (강퇴 등, owner/admin만)
    // ──────────────────────────
    @Transactional
    public ActivityMemberResponse updateActivityMemberStatus(
            Long clubId,
            Long activityMemberId,
            ActivityMemberStatus status
    ) {
        Long currentUserId = SecurityUtil.getCurrentUserId();

        ActivityMember am = activityMemberRepository.findById(activityMemberId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 활동 멤버입니다."));

        validateClubManager(am.getActivity().getClub().getId(), currentUserId);

        if (am.getStatus() == ActivityMemberStatus.JOINED
                && (status == ActivityMemberStatus.CANCELED || status == ActivityMemberStatus.KICKED)) {
            am.getActivity().decreaseCurrentPeople();
        }

        am.changeStatus(status);

        return ActivityMemberResponse.from(am);
    }

    // ──────────────────────────
    // 공통: 클럽 매니저(owner/admin) 권한 체크
    // ──────────────────────────
    private void validateClubManager(Long clubId, Long userId) {
        ClubMember cm = clubMemberRepository.findByClubIdAndUserId(clubId, userId)
                .orElseThrow(() -> new AccessDeniedException("클럽 멤버가 아닙니다."));

        String role = cm.getRole(); // "owner" / "admin" / "member" 등

        if (!"owner".equalsIgnoreCase(role) && !"admin".equalsIgnoreCase(role)) {
            throw new AccessDeniedException("활동 관리 권한이 없습니다 (owner/admin만 가능).");
        }
    }
}
