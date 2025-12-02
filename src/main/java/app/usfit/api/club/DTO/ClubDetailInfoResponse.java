package app.usfit.api.club.DTO;

import java.time.LocalDate;
import java.util.List;

import app.usfit.api.club.entity.Club;
import app.usfit.api.common.enums.ClubMemberRole;
import app.usfit.api.facility.dto.FacilityDetailDto;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ClubDetailInfoResponse{
    private static final String S3_BASE_URL =
            "https://usfit-s3-bucket.s3.ap-southeast-2.amazonaws.com/";

    private String name; // 동호회 이름
    private String description; // 동호회 설명
    private String mainImageUrl; //동호회 메인 이미지 URL
    private int currMemberCount;
    private int memberLimit;
    private String phoneNumber;
    private LocalDate createAt;
    private String snsLink;

    // 메인 시설 정보
    private FacilityDetailDto mainFacility;

    // 동호회가 제공하는 스포츠 종목 리스트
    private List<ClubSportResponse> sports;

    // 이 동호회에 내 권한이 뭔지
    private ClubMemberRole myRoleInClub; // MEMBER, ADMIN, NONE

    /**
     * Club → ClubDetailInfoResponse 변환용 팩토리 메서드
     */
    public static ClubDetailInfoResponse from(
            Club club,
            FacilityDetailDto facility,
            List<ClubSportResponse> sports,
            ClubMemberRole myRoleInClub
    ) {
        String mainImageUrl = null;

        String raw = club.getClubMainImageUrl();
        if (raw != null && !raw.isBlank()) {
            // 이미 http로 시작하면 풀 URL
            if (raw.startsWith("http")) {
                mainImageUrl = raw;
            } else {
                mainImageUrl = S3_BASE_URL + raw;
            }
        }

        return ClubDetailInfoResponse.builder()
                .name(club.getName())
                .description(club.getDescription())
                .mainImageUrl(mainImageUrl)
                .currMemberCount(
                        club.getMembers() == null ? 0 : club.getMembers().size()
                )
                .memberLimit(club.getMemberLimit() == null ? 0 : club.getMemberLimit())
                .phoneNumber(club.getPhoneNumber())
                .createAt(
                        club.getCreatedAt() == null
                                ? null
                                : club.getCreatedAt().toLocalDate()
                )
                .snsLink(club.getSnsLink())
                .mainFacility(facility)
                .sports(sports)
                .myRoleInClub(myRoleInClub) // 기본값 null
                .build();
    }

}
