package com.weaveing.controller;

import com.weaveing.entity.Pattern;
import com.weaveing.entity.Purchase;
import com.weaveing.entity.Review;
import com.weaveing.entity.User;
import com.weaveing.repository.PatternRepository;
import com.weaveing.repository.PurchaseRepository;
import com.weaveing.repository.ReviewRepository;
import com.weaveing.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;

@Controller
public class ReviewController {

    private final PatternRepository patternRepository;
    private final UserRepository userRepository;
    private final PurchaseRepository purchaseRepository;
    private final ReviewRepository reviewRepository;

    public ReviewController(
            PatternRepository patternRepository,
            UserRepository userRepository,
            PurchaseRepository purchaseRepository,
            ReviewRepository reviewRepository) {

        this.patternRepository = patternRepository;
        this.userRepository = userRepository;
        this.purchaseRepository = purchaseRepository;
        this.reviewRepository = reviewRepository;
    }

    @PostMapping("/patterns/{patternId}/reviews")
    public String createReview(
            @PathVariable Long patternId,
            @RequestParam int rating,
            @RequestParam(required = false) String comment,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {

        Pattern pattern = getApprovedPattern(patternId);
        User user = getCurrentUser(authentication);

        if (!isEligible(user, pattern)) {
            redirectAttributes.addFlashAttribute(
                    "reviewError",
                    "Only users who purchased this pattern can review it."
            );
            return redirectToPattern(patternId);
        }

        if (reviewRepository.existsByReviewerAndPattern(user, pattern)) {
            redirectAttributes.addFlashAttribute(
                    "reviewError",
                    "You have already reviewed this pattern."
            );
            return redirectToPattern(patternId);
        }

        if (rating < 1 || rating > 5) {
            redirectAttributes.addFlashAttribute(
                    "reviewError",
                    "Please choose a rating from 1 to 5 stars."
            );
            return redirectToPattern(patternId);
        }

        Review review = new Review();
        review.setReviewer(user);
        review.setPattern(pattern);
        review.setRating(rating);
        review.setComment(cleanComment(comment));
        reviewRepository.save(review);

        redirectAttributes.addFlashAttribute(
                "reviewSuccess",
                "Your review has been posted."
        );

        return redirectToPattern(patternId);
    }

    @PostMapping("/patterns/{patternId}/reviews/{reviewId}/edit")
    public String editReview(
            @PathVariable Long patternId,
            @PathVariable Long reviewId,
            @RequestParam int rating,
            @RequestParam(required = false) String comment,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {

        Pattern pattern = getApprovedPattern(patternId);
        User user = getCurrentUser(authentication);

        Review review = reviewRepository.findById(reviewId).orElse(null);

        if (review == null ||
                !review.getPattern().getId().equals(pattern.getId()) ||
                !review.getReviewer().getId().equals(user.getId())) {

            redirectAttributes.addFlashAttribute(
                    "reviewError",
                    "You can only edit your own review."
            );
            return redirectToPattern(patternId);
        }

        if (rating < 1 || rating > 5) {
            redirectAttributes.addFlashAttribute(
                    "reviewError",
                    "Please choose a rating from 1 to 5 stars."
            );
            return redirectToPattern(patternId);
        }

        review.setRating(rating);
        review.setComment(cleanComment(comment));
        review.setUpdatedAt(LocalDateTime.now());
        reviewRepository.save(review);

        redirectAttributes.addFlashAttribute(
                "reviewSuccess",
                "Your review has been updated."
        );

        return redirectToPattern(patternId);
    }

    @PostMapping("/patterns/{patternId}/reviews/{reviewId}/delete")
    public String deleteReview(
            @PathVariable Long patternId,
            @PathVariable Long reviewId,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {

        getApprovedPattern(patternId);
        User user = getCurrentUser(authentication);

        Review review = reviewRepository.findById(reviewId).orElse(null);

        if (review == null ||
                !review.getPattern().getId().equals(patternId) ||
                !review.getReviewer().getId().equals(user.getId())) {

            redirectAttributes.addFlashAttribute(
                    "reviewError",
                    "You can only delete your own review."
            );
            return redirectToPattern(patternId);
        }

        reviewRepository.delete(review);

        redirectAttributes.addFlashAttribute(
                "reviewSuccess",
                "Your review has been deleted."
        );

        return redirectToPattern(patternId);
    }

    private Pattern getApprovedPattern(Long patternId) {
        Pattern pattern = patternRepository.findById(patternId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Pattern not found")
                );

        if (pattern.getApprovalStatus() != Pattern.ApprovalStatus.APPROVED) {
            throw new IllegalArgumentException("Pattern is not available");
        }

        return pattern;
    }

    private User getCurrentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException("Login required");
        }

        return userRepository
                .findByUsername(authentication.getName())
                .orElseThrow(() ->
                        new IllegalStateException("Logged-in user not found")
                );
    }

    private boolean isEligible(User user, Pattern pattern) {
        if (pattern.getCreator() == null ||
                pattern.getCreator().getId().equals(user.getId())) {
            return false;
        }

        return purchaseRepository.existsByBuyerAndPatternAndPaymentStatus(
                user,
                pattern,
                Purchase.PaymentStatus.VERIFIED
        );
    }

    private String cleanComment(String comment) {
        if (comment == null) {
            return "";
        }

        String cleaned = comment.trim();

        return cleaned.length() > 1000
                ? cleaned.substring(0, 1000)
                : cleaned;
    }

    private String redirectToPattern(Long patternId) {
        return "redirect:/patterns/" + patternId + "#reviews";
    }
}
