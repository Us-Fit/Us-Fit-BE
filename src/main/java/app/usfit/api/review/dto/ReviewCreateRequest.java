package app.usfit.api.review.dto;

import app.usfit.api.review.entity.ReviewTargetType;
import lombok.*;

@Getter
@Setter
public class ReviewCreateRequest {

    private ReviewTargetType targetType;   // COURSE / FACILITY
    private Long targetId;                 // entity id
    private Integer rating;
    private String comment;
}