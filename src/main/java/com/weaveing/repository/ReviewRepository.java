package com.weaveing.repository;

import com.weaveing.entity.Pattern;
import com.weaveing.entity.Review;
import com.weaveing.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    List<Review> findByPatternOrderByCreatedAtDesc(Pattern pattern);

    Optional<Review> findByReviewerAndPattern(User reviewer, Pattern pattern);

    long countByPattern(Pattern pattern);

    boolean existsByReviewerAndPattern(User reviewer, Pattern pattern);

    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.pattern = :pattern")
    Double getAverageRatingByPattern(@Param("pattern") Pattern pattern);
}
