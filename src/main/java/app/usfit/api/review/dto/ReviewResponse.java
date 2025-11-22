package app.usfit.api.review.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class ReviewResponse {

    private Long reviewId;
    private Integer rating;
    private String comment;
    private List<String> imageUrls;

    public static ReviewResponse from(ReviewDto dto) {
        return ReviewResponse.builder()
                .reviewId(dto.getReviewId())
                .rating(dto.getRating())
                .comment(dto.getComment())
                .imageUrls(dto.getImageUrls())
                .build();
    }
}