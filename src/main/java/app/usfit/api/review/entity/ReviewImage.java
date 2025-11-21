package app.usfit.api.review.entity;

import jakarta.persistence.*;

@Entity
public class ReviewImage {
    @Id @GeneratedValue
    private Long id;

    @ManyToOne
    @JoinColumn(name = "review_id")
    private Review review;

    private String imageUrl;
}
