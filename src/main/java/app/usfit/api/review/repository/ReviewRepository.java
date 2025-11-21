package app.usfit.api.review.repository;

import app.usfit.api.review.entity.Review;
import app.usfit.api.review.entity.ReviewTargetType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    List<Review> findByTargetTypeAndTargetId(ReviewTargetType targetType, Long targetId);
}
