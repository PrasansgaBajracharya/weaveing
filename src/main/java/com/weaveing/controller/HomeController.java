package com.weaveing.controller;

import com.weaveing.entity.Pattern;
import com.weaveing.entity.Purchase;
import com.weaveing.entity.User;
import com.weaveing.repository.PatternRepository;
import com.weaveing.repository.PurchaseRepository;
import com.weaveing.repository.UserRepository;
import com.weaveing.repository.WishlistRepository;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Controller
public class HomeController {

    private final UserRepository userRepository;
    private final PatternRepository patternRepository;
    private final PurchaseRepository purchaseRepository;
    private final WishlistRepository wishlistRepository;

    public HomeController(
            UserRepository userRepository,
            PatternRepository patternRepository,
            PurchaseRepository purchaseRepository,
            WishlistRepository wishlistRepository) {

        this.userRepository = userRepository;
        this.patternRepository = patternRepository;
        this.purchaseRepository = purchaseRepository;
        this.wishlistRepository = wishlistRepository;
    }

    @GetMapping("/home")
    public String home(
            @RequestParam(defaultValue = "") String search,
            @RequestParam(defaultValue = "all") String category,
            @RequestParam(defaultValue = "all") String difficulty,
            @RequestParam(defaultValue = "all") String priceType,
            @RequestParam(defaultValue = "newest") String sort,
            Authentication authentication,
            Model model) {

        User user = userRepository
                .findByUsername(authentication.getName())
                .orElseThrow(() ->
                        new IllegalStateException(
                                "Logged-in user not found"
                        )
                );

        String cleanSearch =
                search == null
                        ? ""
                        : search.trim();

        if (cleanSearch.length() > 100) {
            cleanSearch = cleanSearch.substring(0, 100);
        }

        final String searchTerm = cleanSearch;

        if (!Pattern.CATEGORIES.contains(category)) {
            if (!"all".equals(category)) {
                category = "all";
            }
        }

        if (!List.of(
                "all",
                "Beginner",
                "Intermediate",
                "Advanced"
        ).contains(difficulty)) {
            difficulty = "all";
        }

        if (!List.of(
                "all",
                "free",
                "paid"
        ).contains(priceType)) {
            priceType = "all";
        }

        if (!List.of(
                "newest",
                "popular",
                "downloads",
                "price-low",
                "price-high"
        ).contains(sort)) {
            sort = "newest";
        }

        Sort patternSort;

        switch (sort) {
            case "popular" ->
                    patternSort =
                            Sort.by(
                                    Sort.Order.desc("downloads"),
                                    Sort.Order.desc("submittedAt")
                            );

            case "downloads" ->
                    patternSort =
                            Sort.by(
                                    Sort.Order.desc("downloads"),
                                    Sort.Order.desc("submittedAt")
                            );

            case "price-low" ->
                    patternSort =
                            Sort.by(
                                    Sort.Order.asc("price"),
                                    Sort.Order.desc("submittedAt")
                            );

            case "price-high" ->
                    patternSort =
                            Sort.by(
                                    Sort.Order.desc("price"),
                                    Sort.Order.desc("submittedAt")
                            );

            default ->
                    patternSort =
                            Sort.by(
                                    Sort.Order.desc("submittedAt")
                            );
        }

        List<Pattern> approvedPatterns =
                patternRepository.searchApprovedPatterns(
                        Pattern.ApprovalStatus.APPROVED,
                        category,
                        difficulty,
                        priceType,
                        patternSort
                );

        approvedPatterns =
                approvedPatterns.stream()
                        .filter(pattern -> matchesSearch(pattern, searchTerm))
                        .toList();

        for (Pattern pattern : approvedPatterns) {

            long purchaseCount =
                    purchaseRepository
                            .countByPatternAndPaymentStatus(
                                    pattern,
                                    Purchase.PaymentStatus.VERIFIED
                            );

            pattern.setPurchaseCount(purchaseCount);
        }

        Set<Long> wishlistPatternIds =
                wishlistRepository
                        .findByUserOrderByCreatedAtDesc(user)
                        .stream()
                        .map(item -> item.getPattern().getId())
                        .collect(Collectors.toSet());

        Map<Long, Long> wishlistCounts =
                new HashMap<>();

        for (Pattern pattern : approvedPatterns) {

            wishlistCounts.put(
                    pattern.getId(),
                    wishlistRepository.countByPattern(pattern)
            );
        }

        model.addAttribute("user", user);
        model.addAttribute("approvedPatterns", approvedPatterns);
        model.addAttribute("resultCount", approvedPatterns.size());
        model.addAttribute("wishlistPatternIds", wishlistPatternIds);
        model.addAttribute("wishlistCounts", wishlistCounts);
        model.addAttribute("categories", Pattern.CATEGORIES);

        model.addAttribute("search", cleanSearch);
        model.addAttribute("selectedCategory", category);
        model.addAttribute("selectedDifficulty", difficulty);
        model.addAttribute("selectedPriceType", priceType);
        model.addAttribute("selectedSort", sort);

        int activeFilterCount = 0;

        if (!cleanSearch.isBlank()) {
            activeFilterCount++;
        }

        if (!"all".equals(category)) {
            activeFilterCount++;
        }

        if (!"all".equals(difficulty)) {
            activeFilterCount++;
        }

        if (!"all".equals(priceType)) {
            activeFilterCount++;
        }

        if (!"newest".equals(sort)) {
            activeFilterCount++;
        }

        model.addAttribute("activeFilterCount", activeFilterCount);

        StringBuilder filterSummary = new StringBuilder();

        if (!cleanSearch.isBlank()) {
            filterSummary.append("Search: ").append(cleanSearch);
        }

        if (!"all".equals(category)) {
            appendSummaryPart(filterSummary, category);
        }

        if (!"all".equals(difficulty)) {
            appendSummaryPart(filterSummary, difficulty);
        }

        if (!"all".equals(priceType)) {
            appendSummaryPart(
                    filterSummary,
                    "free".equals(priceType) ? "Free" : "Paid"
            );
        }

        if (!"newest".equals(sort)) {
            appendSummaryPart(
                    filterSummary,
                    switch (sort) {
                        case "popular" -> "Most popular";
                        case "downloads" -> "Most downloaded";
                        case "price-low" -> "Price: Low to High";
                        case "price-high" -> "Price: High to Low";
                        default -> "Newest";
                    }
            );
        }

        model.addAttribute(
                "filterSummary",
                filterSummary.isEmpty()
                        ? "All patterns"
                        : filterSummary.toString()
        );

        return "home";
    }

    private boolean matchesSearch(
            Pattern pattern,
            String search) {

        if (search == null || search.isBlank()) {
            return true;
        }

        String[] terms =
                search.toLowerCase(Locale.ROOT)
                        .trim()
                        .split("\\s+");

        String searchableText =
                String.join(" ",
                        pattern.getTitle(),
                        pattern.getDescription(),
                        pattern.getCategory(),
                        pattern.getCreator().getName(),
                        pattern.getCreator().getUsername()
                ).toLowerCase(Locale.ROOT);

        for (String term : terms) {
            if (!searchableText.contains(term)) {
                return false;
            }
        }

        return true;
    }

    private void appendSummaryPart(
            StringBuilder summary,
            String value) {

        if (!summary.isEmpty()) {
            summary.append(" · ");
        }

        summary.append(value);
    }
}