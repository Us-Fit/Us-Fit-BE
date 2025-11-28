package app.usfit.api.club.activity.dto;

import app.usfit.api.club.activity.entity.ActivityMember;
import app.usfit.api.club.activity.entity.ActivityMemberStatus;
import app.usfit.api.user.entity.User;
import app.usfit.api.user.entity.UserProfile;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "활동 참가자 응답 DTO")
public class ActivityMemberResponse {

    @Schema(description = "활동 멤버 ID", example = "10")
    private Long id; //activityMemberId

    @Schema(description = "유저 ID", example = "6")
    private Long userId;

    @Schema(description = "닉네임(또는 이름)", example = "농구좋아하는코더")
    private String nickname;

    @Schema(description = "멤버 상태", example = "JOINED")
    private ActivityMemberStatus status;

    public static ActivityMemberResponse from(ActivityMember am) {

        User user = am.getUser();
        UserProfile profile = user.getProfile(); // 연관관계 이름에 맞게 수정

        return ActivityMemberResponse.builder()
                .id(am.getId())
                .userId(am.getUser().getId())
                .nickname(profile.getNickname()) // User 엔티티에 맞게 필드명 수정
                .status(am.getStatus())
                .build();
    }
}
