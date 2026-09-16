package com.weaveing.controller;

import com.weaveing.entity.Pattern;
import com.weaveing.entity.User;
import com.weaveing.repository.PatternRepository;
import com.weaveing.repository.ReviewRepository;
import com.weaveing.repository.UserRepository;
import com.weaveing.repository.WishlistRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@Controller
public class PublicProfileController {
    private final UserRepository userRepository;
    private final PatternRepository patternRepository;
    private final ReviewRepository reviewRepository;
    private final WishlistRepository wishlistRepository;

    public PublicProfileController(UserRepository userRepository, PatternRepository patternRepository, ReviewRepository reviewRepository, WishlistRepository wishlistRepository) {
        this.userRepository = userRepository;
        this.patternRepository = patternRepository;
        this.reviewRepository = reviewRepository;
        this.wishlistRepository = wishlistRepository;
    }

    @GetMapping("/users/{username}")
    public String publicProfile(@PathVariable String username, Model model) {
        User profileUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        List<Pattern> publishedPatterns = patternRepository.findByCreatorAndApprovalStatus(profileUser, Pattern.ApprovalStatus.APPROVED);
        long reviewCount = reviewRepository.countByReviewer(profileUser);
        long totalSaves = 0;
        long totalDownloads = 0;
        long totalReviews = 0;
        double ratingSum = 0.0;

        for (Pattern pattern : publishedPatterns) {
            totalSaves += wishlistRepository.countByPattern(pattern);
            totalDownloads += pattern.getDownloads();
            long patternReviews = reviewRepository.countByPattern(pattern);
            Double average = reviewRepository.getAverageRatingByPattern(pattern);
            totalReviews += patternReviews;
            if (average != null) {
                ratingSum += average * patternReviews;
            }
        }

        double averageRating = totalReviews == 0 ? 0.0 : ratingSum / totalReviews;
        model.addAttribute("profileUser", profileUser);
        model.addAttribute("publishedPatterns", publishedPatterns);
        model.addAttribute("publishedPatternCount", publishedPatterns.size());
        model.addAttribute("reviewCount", reviewCount);
        model.addAttribute("totalSaves", totalSaves);
        model.addAttribute("totalDownloads", totalDownloads);
        model.addAttribute("totalReviews", totalReviews);
        model.addAttribute("averageRating", averageRating);
        return "public-profile";
    }
}
