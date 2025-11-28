package app.usfit.api.review.service;

import app.usfit.api.review.dto.ReviewCreateRequest;
import app.usfit.api.review.dto.ReviewDto;
import app.usfit.api.review.dto.ReviewResponse;
import app.usfit.api.review.dto.ReviewUpdateRequest;
import app.usfit.api.review.entity.Review;
import app.usfit.api.review.entity.ReviewImage;
import app.usfit.api.review.entity.ReviewTargetType;
import app.usfit.api.review.repository.ReviewRepository;
import app.usfit.api.user.entity.UserProfile;
import app.usfit.api.user.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private static final String S3_BASE_URL = "https://usfit-s3-bucket.s3.ap-southeast-2.amazonaws.com/";
    private final ReviewRepository reviewRepository;
    private final ReviewImageService reviewImageService;
    private final UserProfileRepository userProfileRepository;

    //review 생성
    public ReviewResponse createReview(ReviewCreateRequest request,
                                       List<MultipartFile> images,
                                       Long userId) {

        // 1. 리뷰 저장
        Review review = Review.builder()
                .targetType(request.getTargetType())
                .targetId(request.getTargetId())
                .userId(userId)
                .rating(request.getRating())
                .comment(request.getComment())
                .build();

        reviewRepository.save(review);

        // 2. 이미지 업로드 → ReviewImage 생성
        if (images != null) {
            for (MultipartFile image : images) {

                String key = reviewImageService.uploadReviewImage(image, review.getId());

                ReviewImage reviewImage = ReviewImage.builder()
                        .review(review)
                        .imageUrl(key)
                        .build();

                review.getImages().add(reviewImage);
            }
        }

        reviewRepository.save(review);

        // 3. Response 생성
        List<String> imageUrls = review.getImages().stream()
                .map(ReviewImage::getImageUrl)
                .toList();

        return ReviewResponse.builder()
                .reviewId(review.getId())
                .rating(review.getRating())
                .comment(review.getComment())
                .imageUrls(imageUrls)
                .build();
    }

    //review 조회
    public List<ReviewDto> getReviews(
            ReviewTargetType targetType,
            Long targetId,
            Long currentUserId   // 👈 추가
    ) {
        List<Review> reviews =
                reviewRepository.findByTargetTypeAndTargetId(targetType, targetId);

        return reviews.stream()
                .map(review -> {
                    // 작성자 프로필 조회
                    Long authorId = review.getUserId();
                    UserProfile profile = userProfileRepository.findById(authorId)
                            .orElse(null);

                    return ReviewDto.fromEntity(review, currentUserId, profile);
                })
                .toList();
    }

    @Transactional
    public ReviewResponse updateReview(
            Long reviewId,
            ReviewUpdateRequest request,
            List<MultipartFile> newImages,
            Long userId
    ) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("리뷰 없음"));

        validateOwnership(review, userId);

        // 1. 기본 필드 수정
        review.setRating(request.getRating());
        review.setComment(request.getComment());

        // 2. 기존 이미지 삭제 (S3 + DB)
        if (request.getDeleteImageUrls() != null) {
            for (String url : request.getDeleteImageUrls()) {
                // url → key 변환
                String key = url.replace(S3_BASE_URL, "");

                // S3 삭제
                reviewImageService.deleteImage(key);

                // DB 삭제
                review.getImages()
                        .removeIf(img -> img.getImageUrl().equals(key));
            }
        }

        // 3. 새 이미지 추가
        if (newImages != null) {
            for (MultipartFile file : newImages) {
                String key = reviewImageService.uploadReviewImage(file, reviewId);

                ReviewImage reviewImage = ReviewImage.builder()
                        .review(review)
                        .imageUrl(key)
                        .build();

                review.getImages().add(reviewImage);
            }
        }

        // 변경사항 자동 저장됨 (Transactional)

        return ReviewResponse.from(ReviewDto.fromEntity(review));
    }

    @Transactional
    public void deleteReview(Long reviewId, Long userId) {

        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("리뷰 없음"));

        validateOwnership(review, userId);

        // 1. 리뷰 이미지 전부 S3에서 삭제
        for (ReviewImage img : review.getImages()) {
            reviewImageService.deleteImage(img.getImageUrl());
        }

        // 2. 리뷰 삭제 (연관 이미지도 cascade로 자동 삭제)
        reviewRepository.delete(review);
    }

    private void validateOwnership(Review review, Long userId) {
        if (!review.getUserId().equals(userId)) {
            throw new AccessDeniedException("본인 리뷰만 수정/삭제할 수 있습니다.");
        }
    }
}