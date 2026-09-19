package com.weaveing.dto.api;

import com.weaveing.entity.Pattern;

import java.time.LocalDateTime;

public record PatternApiResponse(
        Long id,
        String title,
        String description,
        String creatorUsername,
        String category,
        String difficulty,
        boolean free,
        double price,
        double originalPrice,
        double discountPercent,
        String imagePath,
        long downloads,
        LocalDateTime submittedAt
) {

    public static PatternApiResponse from(Pattern pattern) {
        return new PatternApiResponse(
                pattern.getId(),
                pattern.getTitle(),
                pattern.getDescription(),
                pattern.getCreator() == null
                        ? null
                        : pattern.getCreator().getUsername(),
                pattern.getCategoryLabel(),
                pattern.getDifficulty(),
                pattern.isFree(),
                pattern.getPrice(),
                pattern.getOriginalPrice(),
                pattern.getDiscountPercent(),
                pattern.getImagePath(),
                pattern.getDownloads(),
                pattern.getSubmittedAt()
        );
    }
}
