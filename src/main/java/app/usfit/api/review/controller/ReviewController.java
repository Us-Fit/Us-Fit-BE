package app.usfit.api.review.controller;

import app.usfit.api.review.dto.ReviewCreateRequest;
import app.usfit.api.review.dto.ReviewDto;
import app.usfit.api.review.dto.ReviewResponse;
import app.usfit.api.review.dto.ReviewUpdateRequest;
import app.usfit.api.review.entity.ReviewTargetType;
import app.usfit.api.review.service.ReviewImageService;
import app.usfit.api.review.service.ReviewService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/review")
public class ReviewController {
    private final ReviewImageService reviewImageService;
    private final ReviewService reviewService;
    private final ObjectMapper objectMapper;

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
            @RequestPart("request") String requestJson,
            @RequestPart(value = "images", required = false) List<MultipartFile> images
    ) throws Exception {

        ObjectMapper mapper = new ObjectMapper();
        ReviewCreateRequest request = mapper.readValue(requestJson, ReviewCreateRequest.class);

        return reviewService.createReview(request, images);
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
            @PathVariable Long reviewId,
            @RequestPart("request") String requestJson,   // 문자열로 받기
            @RequestPart(value = "images", required = false) List<MultipartFile> newImages
    ) throws JsonProcessingException {

        // JSON 문자열 → DTO 로 수동 변환
        ReviewUpdateRequest request = objectMapper.readValue(requestJson, ReviewUpdateRequest.class);

        return reviewService.updateReview(reviewId, request, newImages);
    }


    // 리뷰 삭제
    @DeleteMapping("/{reviewId}")
    public void deleteReview(@PathVariable Long reviewId) {
        reviewService.deleteReview(reviewId);
    }
}
