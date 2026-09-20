package com.weaveing.service;

import com.weaveing.dto.LeaderboardEntry;
import com.weaveing.entity.Pattern;
import com.weaveing.entity.Purchase;
import com.weaveing.entity.User;
import com.weaveing.repository.PatternRepository;
import com.weaveing.repository.PurchaseRepository;
import com.weaveing.repository.UserRepository;
import com.weaveing.repository.WishlistRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class LeaderboardService {

    private static final long APPROVED_PATTERN_XP = 100;
    private static final long VERIFIED_SALE_XP = 50;
    private static final long DOWNLOAD_XP = 10;
    private static final long WISHLIST_XP = 5;
    private static final int LIMIT = 5;

    private final UserRepository userRepository;
    private final PatternRepository patternRepository;
    private final PurchaseRepository purchaseRepository;
    private final WishlistRepository wishlistRepository;

    public LeaderboardService(
            UserRepository userRepository,
            PatternRepository patternRepository,
            PurchaseRepository purchaseRepository,
            WishlistRepository wishlistRepository) {

        this.userRepository = userRepository;
        this.patternRepository = patternRepository;
        this.purchaseRepository = purchaseRepository;
        this.wishlistRepository = wishlistRepository;
    }

    public List<LeaderboardEntry> getTopCreators() {

        List<Pattern> patterns =
                patternRepository.findByApprovalStatus(
                        Pattern.ApprovalStatus.APPROVED
                )
                .stream()
                .filter(pattern -> pattern.getRemovedAt() == null)
                .toList();

        Map<Long, Long> scores = buildCreatorScores(patterns);

        return sortUsersByScore(scores, "XP");
    }

    public List<LeaderboardEntry> getTopSellers() {

        List<Purchase> purchases =
                purchaseRepository.findAll()
                        .stream()
                        .filter(purchase ->
                                purchase.getPaymentStatus() ==
                                        Purchase.PaymentStatus.VERIFIED)
                        .toList();

        Map<Long, Long> sales = new HashMap<>();

        for (Purchase purchase : purchases) {

            Pattern pattern = purchase.getPattern();

            if (pattern == null || pattern.getCreator() == null) {
                continue;
            }

            Long creatorId = pattern.getCreator().getId();

            sales.merge(creatorId, 1L, Long::sum);
        }

        return sortUsersByScore(sales, "sales");
    }

    public List<LeaderboardEntry> getMostDownloaded() {

        List<Pattern> patterns =
                patternRepository.findByApprovalStatus(
                        Pattern.ApprovalStatus.APPROVED
                )
                .stream()
                .filter(pattern -> pattern.getRemovedAt() == null)
                .toList();

        Map<Long, Long> downloads = new HashMap<>();

        for (Pattern pattern : patterns) {

            if (pattern.getCreator() == null) {
                continue;
            }

            downloads.merge(
                    pattern.getCreator().getId(),
                    pattern.getDownloads(),
                    Long::sum
            );
        }

        return sortUsersByScore(downloads, "downloads");
    }

    private Map<Long, Long> buildCreatorScores(
            List<Pattern> patterns) {

        Map<Long, Long> scores = new HashMap<>();

        for (Pattern pattern : patterns) {

            if (pattern.getCreator() == null) {
                continue;
            }

            Long creatorId = pattern.getCreator().getId();

            scores.merge(
                    creatorId,
                    APPROVED_PATTERN_XP,
                    Long::sum
            );

            scores.merge(
                    creatorId,
                    pattern.getDownloads() * DOWNLOAD_XP,
                    Long::sum
            );

            long wishlistCount =
                    wishlistRepository.countByPattern(pattern);

            scores.merge(
                    creatorId,
                    wishlistCount * WISHLIST_XP,
                    Long::sum
            );
        }

        List<Purchase> verifiedPurchases =
                purchaseRepository.findAll()
                        .stream()
                        .filter(purchase ->
                                purchase.getPaymentStatus() ==
                                        Purchase.PaymentStatus.VERIFIED)
                        .toList();

        for (Purchase purchase : verifiedPurchases) {

            Pattern pattern = purchase.getPattern();

            if (pattern == null || pattern.getCreator() == null) {
                continue;
            }

            scores.merge(
                    pattern.getCreator().getId(),
                    VERIFIED_SALE_XP,
                    Long::sum
            );
        }

        return scores;
    }

    private List<LeaderboardEntry> sortUsersByScore(
            Map<Long, Long> scores,
            String metric) {

        if (scores.isEmpty()) {
            return List.of();
        }

        List<User> users =
                userRepository.findAll()
                        .stream()
                        .filter(user -> !user.isAdmin())
                        .toList();

        List<LeaderboardEntry> entries = new ArrayList<>();

        for (User user : users) {

            long score = scores.getOrDefault(user.getId(), 0L);

            if (score > 0) {
                entries.add(
                        new LeaderboardEntry(
                                user,
                                score,
                                metric
                        )
                );
            }
        }

        entries.sort(
                Comparator.comparingLong(
                                LeaderboardEntry::getScore
                        )
                        .reversed()
                        .thenComparing(
                                entry -> entry.getUser().getUsername(),
                                String.CASE_INSENSITIVE_ORDER
                        )
        );

        return entries.stream()
                .limit(LIMIT)
                .toList();
    }
}
