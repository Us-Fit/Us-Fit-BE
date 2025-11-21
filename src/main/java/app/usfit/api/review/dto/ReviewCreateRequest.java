package app.usfit.api.review.dto;

import app.usfit.api.review.entity.ReviewTargetType;
import lombok.*;

@Getter
@Setter
public class ReviewCreateRequest {

    private ReviewTargetType targetType;   // COURSE / FACILITY
    private Long targetId;                 // entity id
    private Long userId;                   // 로그인 기능 붙으면 FE가 토큰에서 전달
    private Integer rating;
    private String comment;
}