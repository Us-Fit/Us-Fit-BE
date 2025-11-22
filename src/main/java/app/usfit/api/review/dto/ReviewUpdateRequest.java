package app.usfit.api.review.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
public class ReviewUpdateRequest {
    private Integer rating;
    private String comment;

    // 삭제할 이미지 URL들
    private List<String> deleteImageUrls;
}