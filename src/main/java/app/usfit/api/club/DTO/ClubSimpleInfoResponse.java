package app.usfit.api.club.DTO;

import java.util.List;

import app.usfit.api.club.entity.Club;
import app.usfit.api.user.dto.SimpleProfileResponse;

public record ClubSimpleInfoResponse(
        Long id,
        String name,
        String regionName,
        Integer memberLimit,
        Boolean visibility,
        String status,
        SimpleProfileResponse owner,
        Long mainFacilityId,
        Integer memberCount,
        List<ClubSportResponse> sports,
        String clubMainImageUrl
) {
    private static final String S3_BASE_URL =
            "https://usfit-s3-bucket.s3.ap-southeast-2.amazonaws.com/";

    public static ClubSimpleInfoResponse from(
            Club club,
            SimpleProfileResponse owner,
            List<ClubSportResponse> sports
    ) {
        // 메인 이미지 URL 처리 (상대 경로 / 풀 URL / null 모두 대응)
        String mainImageUrl = null;
        String raw = club.getClubMainImageUrl();
        if (raw != null && !raw.isBlank()) {
            if (raw.startsWith("http")) {
                mainImageUrl = raw;
            } else {
                mainImageUrl = S3_BASE_URL + raw;
            }
        }

        // mainFacilityId
        Long mainFacilityId = club.getMainFacility() != null
                ? club.getMainFacility().getId()
                : null;

        // memberCount
        Integer memberCount = club.getMembers() != null
                ? club.getMembers().size()
                : 0;

        return new ClubSimpleInfoResponse(
                club.getId(),
                club.getName(),
                club.getRegionName(),
                club.getMemberLimit(),
                club.getVisibility(),
                club.getStatus(),
                owner,
                mainFacilityId,
                memberCount,
                sports,
                mainImageUrl
        );
    }
}
