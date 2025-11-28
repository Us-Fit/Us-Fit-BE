package app.usfit.api.review.dto;

import app.usfit.api.review.entity.Review;
import app.usfit.api.user.entity.UserProfile;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class ReviewDto {

    private static final String S3_BASE_URL =
            "https://usfit-s3-bucket.s3.ap-southeast-2.amazonaws.com/";

    private Long reviewId;
    private Integer rating;
    private String comment;
    private List<String> imageUrls;

    private Long authorId;
    private String nickname;
    private String profileImageUrl;
    private boolean isMine;

    /**
     * 기존 코드와의 호환용
     * - updateReview() 같은 곳에서 사용
     * - isMine, nickname, profileImageUrl은 아직 필요 없을 때 사용
     */
    public static ReviewDto fromEntity(Review review) {
        List<String> imageUrls = review.getImages().stream()
                .map(img -> S3_BASE_URL + img.getImageUrl())  // key -> 전체 URL
                .toList();

        Long authorId = review.getUserId();

        return ReviewDto.builder()
                .reviewId(review.getId())
                .rating(review.getRating())
                .comment(review.getComment())
                .imageUrls(imageUrls)
                .authorId(authorId)
                .nickname(null)
                .profileImageUrl(null)
                .isMine(false)
                .build();
    }

    /**
     * 리뷰 리스트 조회용
     * - 현재 로그인 유저 id를 받아서 isMine 계산
     * - UserProfile에서 nickname, profileImageKey를 받아서 내려줌
     */
    public static ReviewDto fromEntity(
            Review review,
            Long currentUserId,
            UserProfile profile
    ) {
        List<String> imageUrls = review.getImages().stream()
                .map(img -> S3_BASE_URL + img.getImageUrl())
                .toList();

        Long authorId = review.getUserId();

        String profileImageUrl = null;
        if (profile != null && profile.getProfileImageKey() != null) {
            profileImageUrl = S3_BASE_URL + profile.getProfileImageKey();
        }

        return ReviewDto.builder()
                .reviewId(review.getId())
                .rating(review.getRating())
                .comment(review.getComment())
                .imageUrls(imageUrls)
                .authorId(authorId)
                .nickname(profile != null ? profile.getNickname() : null)
                .profileImageUrl(profileImageUrl)
                .isMine(authorId.equals(currentUserId))
                .build();
    }
}
