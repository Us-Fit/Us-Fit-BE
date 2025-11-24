package app.usfit.api.review.controller;

import app.usfit.api.review.dto.ReviewCreateRequest;
import app.usfit.api.review.dto.ReviewDto;
import app.usfit.api.review.dto.ReviewResponse;
import app.usfit.api.review.dto.ReviewUpdateRequest;
import app.usfit.api.review.entity.ReviewTargetType;
import app.usfit.api.review.service.ReviewImageService;
import app.usfit.api.review.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/review")
public class ReviewController {
    private final ReviewImageService reviewImageService;
    private final ReviewService reviewService;

    //review에 이미지 업로드
    @PostMapping("/{reviewId}/images")
    public List<String> uploadReviewImages(
            @PathVariable Long reviewId,
            @RequestPart("images") List<MultipartFile> images
    ) {
        return images.stream()
                .map(img -> reviewImageService.uploadReviewImage(img, reviewId))
                .toList();
    }

    //review 작성
    @PostMapping(consumes = {"multipart/form-data"})
    public ReviewResponse createReview(
            Authentication authentication,
            @RequestPart("request")
            @Parameter(
                    description = "리뷰 본문 데이터",
                    schema = @Schema(implementation = ReviewCreateRequest.class),
                    example = "{\n  \"targetType\": \"COURSE\",\n  \"targetId\": 1,\n  \"rating\": 5,\n  \"comment\": \"좋아요!\"\n}"
            )
            ReviewCreateRequest request,
            @RequestPart(value = "images", required = false) List<MultipartFile> images
    ) {

        Long userId = Long.parseLong(authentication.getName());

        return reviewService.createReview(request, images, userId);
    }

    //review 리스트 받아오기
    @GetMapping
    public List<ReviewDto> getReviews(
            @RequestParam ReviewTargetType targetType,
            @RequestParam Long targetId
    ) {
        return reviewService.getReviews(targetType, targetId);
    }

    // 리뷰 수정
    @PutMapping(value = "/{reviewId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ReviewResponse updateReview(
            Authentication authentication,
            @PathVariable Long reviewId,
            @RequestPart("request")
            @Parameter(
                    description = "리뷰 수정 본문 데이터",
                    schema = @Schema(implementation = ReviewUpdateRequest.class),
                    example = "{\n  \"rating\": 4,\n  \"comment\": \"코멘트 수정\",\n  \"deleteImageUrls\": [\n    \"https://usfit-s3-bucket.s3.ap-southeast-2.amazonaws.com/review/1/img1.png\"\n  ]\n}"
            )
            ReviewUpdateRequest request,
            @RequestPart(value = "images", required = false) List<MultipartFile> newImages
    ) {

        Long userId = Long.parseLong(authentication.getName());

        return reviewService.updateReview(reviewId, request, newImages, userId);
    }


    // 리뷰 삭제
    @DeleteMapping("/{reviewId}")
    public void deleteReview(Authentication authentication, @PathVariable Long reviewId) {
        Long userId = Long.parseLong(authentication.getName());
        reviewService.deleteReview(reviewId, userId);
    }
}
