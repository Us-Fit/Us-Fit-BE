package app.usfit.api.review.dto;

import app.usfit.api.review.entity.Review;
import app.usfit.api.review.entity.ReviewImage;
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

    public static ReviewDto fromEntity(Review review) {
        return ReviewDto.builder()
                .reviewId(review.getId())
                .rating(review.getRating())
                .comment(review.getComment())
                .imageUrls(
                        review.getImages().stream()
                                .map(img -> S3_BASE_URL + img.getImageUrl())  // 전체 URL로 변환!
                                .toList()
                )
                .build();
    }
}