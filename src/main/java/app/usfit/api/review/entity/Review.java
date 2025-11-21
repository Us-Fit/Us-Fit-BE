package app.usfit.api.review.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
public class Review {
    @Id @GeneratedValue
    private Long id;

    private String targetType; // course or facility
    private Long targetId; // courseId or facilityId

    private Long userId;
    private Integer rating;
    private String comment;

    @OneToMany(mappedBy = "review", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ReviewImage> images = new ArrayList<>();

    private LocalDateTime createdDate;
    private LocalDateTime modifiedDate;
}
