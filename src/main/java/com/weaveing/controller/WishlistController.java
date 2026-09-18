package com.weaveing.controller;

import com.weaveing.entity.Pattern;
import com.weaveing.entity.Purchase;
import com.weaveing.entity.User;
import com.weaveing.entity.WishlistItem;
import com.weaveing.repository.PatternRepository;
import com.weaveing.repository.PurchaseRepository;
import com.weaveing.repository.UserRepository;
import com.weaveing.repository.WishlistRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class WishlistController {

    private final WishlistRepository wishlistRepository;
    private final PatternRepository patternRepository;
    private final PurchaseRepository purchaseRepository;
    private final UserRepository userRepository;

    public WishlistController(
            WishlistRepository wishlistRepository,
            PatternRepository patternRepository,
            PurchaseRepository purchaseRepository,
            UserRepository userRepository) {

        this.wishlistRepository = wishlistRepository;
        this.patternRepository = patternRepository;
        this.purchaseRepository = purchaseRepository;
        this.userRepository = userRepository;
    }

    @GetMapping("/wishlist")
    public String wishlist(
            Authentication authentication,
            Model model) {

        User user = findLoggedInUser(authentication);

        List<WishlistItem> wishlistItems =
                wishlistRepository
                        .findByUserOrderByCreatedAtDesc(user);

        wishlistItems.removeIf(item -> {
            Pattern pattern = item.getPattern();
            if (pattern.getRemovedAt() != null) {
                wishlistRepository.delete(item);
                return true;
            }
            return false;
        });

        Map<Long, Long> wishlistCounts =
                new HashMap<>();

        for (WishlistItem item : wishlistItems) {

            Pattern pattern = item.getPattern();

            wishlistCounts.put(
                    pattern.getId(),
                    wishlistRepository.countByPattern(pattern)
            );
        }

        model.addAttribute(
                "wishlistItems",
                wishlistItems
        );

        model.addAttribute(
                "wishlistCounts",
                wishlistCounts
        );

        return "wishlist";
    }

    @PostMapping("/wishlist/toggle/{patternId}")
    @ResponseBody
    public ResponseEntity<?> toggleWishlist(
            @PathVariable Long patternId,
            Authentication authentication) {

        User user = findLoggedInUser(authentication);

        Pattern pattern =
                patternRepository.findById(patternId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Pattern not found"
                                )
                        );

        if (pattern.getApprovalStatus() !=
                Pattern.ApprovalStatus.APPROVED ||
                pattern.getRemovedAt() != null) {

            return ResponseEntity.badRequest()
                    .body(
                            Map.of(
                                    "success",
                                    false
                            )
                    );
        }

        boolean saved;

        if (wishlistRepository
                .existsByUserAndPattern(user, pattern)) {

            wishlistRepository
                    .deleteByUserAndPattern(
                            user,
                            pattern
                    );

            saved = false;

        } else {

            WishlistItem item =
                    new WishlistItem();

            item.setUser(user);
            item.setPattern(pattern);

            wishlistRepository.save(item);

            saved = true;
        }

        long count =
                wishlistRepository
                        .countByPattern(pattern);

        return ResponseEntity.ok(
                Map.of(
                        "success",
                        true,
                        "saved",
                        saved,
                        "count",
                        count
                )
        );
    }

    @PostMapping("/wishlist/remove/{patternId}")
    public String removeWishlist(
            @PathVariable Long patternId,
            Authentication authentication) {

        User user =
                findLoggedInUser(authentication);

        Pattern pattern =
                patternRepository.findById(patternId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Pattern not found"
                                )
                        );

        wishlistRepository.deleteByUserAndPattern(
                user,
                pattern
        );

        return "redirect:/wishlist";
    }

    @PostMapping("/wishlist/delete-all")
    public String deleteAllWishlist(
            Authentication authentication) {

        wishlistRepository.deleteByUser(
                findLoggedInUser(authentication)
        );

        return "redirect:/wishlist";
    }

    @PostMapping("/wishlist/buy/{patternId}")
    public String buyWishlistPattern(
            @PathVariable Long patternId,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {

        User user =
                findLoggedInUser(authentication);

        Pattern pattern =
                patternRepository.findById(patternId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Pattern not found"
                                )
                        );

        if (pattern.getApprovalStatus() !=
                Pattern.ApprovalStatus.APPROVED ||
                pattern.getRemovedAt() != null) {

            wishlistRepository.deleteByUserAndPattern(user, pattern);

            redirectAttributes.addFlashAttribute(
                    "wishlistMessage",
                    "This pattern is no longer available for purchase."
            );

            return "redirect:/wishlist";
        }

        if (pattern.isFree()) {

            redirectAttributes.addFlashAttribute(
                    "purchaseMessage",
                    "This pattern is free and does not require a purchase."
            );

            return "redirect:/patterns/" + patternId;
        }

        boolean alreadyOwned =
                purchaseRepository
                        .existsByBuyerAndPatternAndPaymentStatus(
                                user,
                                pattern,
                                Purchase.PaymentStatus.VERIFIED
                        );

        if (alreadyOwned) {

            wishlistRepository.deleteByUserAndPattern(
                    user,
                    pattern
            );

            redirectAttributes.addFlashAttribute(
                    "purchaseMessage",
                    "You already own this pattern."
            );

            return "redirect:/patterns/" + patternId;
        }

        Purchase purchase =
                purchaseRepository
                        .findByBuyerAndPatternAndPaymentStatus(
                                user,
                                pattern,
                                Purchase.PaymentStatus.PENDING
                        )
                        .orElseGet(Purchase::new);

        purchase.setBuyer(user);
        purchase.setPattern(pattern);
        purchase.setAmount(pattern.getPrice());
        purchase.setPaymentStatus(
                Purchase.PaymentStatus.PENDING
        );

        purchaseRepository.save(purchase);

        return "redirect:/payment/" + purchase.getId();
    }

    @PostMapping("/wishlist/buy-all")
    public String buyAllWishlist(
            Authentication authentication,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        User user =
                findLoggedInUser(authentication);

        List<Long> purchaseIds =
                new ArrayList<>();

        for (WishlistItem item :
                wishlistRepository
                        .findByUserOrderByCreatedAtDesc(user)) {

            Pattern pattern =
                    item.getPattern();

            if (pattern.getApprovalStatus() !=
                    Pattern.ApprovalStatus.APPROVED ||
                    pattern.getRemovedAt() != null ||
                    pattern.isFree()) {

                continue;
            }

            boolean alreadyOwned =
                    purchaseRepository
                            .existsByBuyerAndPatternAndPaymentStatus(
                                    user,
                                    pattern,
                                    Purchase.PaymentStatus.VERIFIED
                            );

            if (alreadyOwned) {
                continue;
            }

            Purchase purchase =
                    purchaseRepository
                            .findByBuyerAndPatternAndPaymentStatus(
                                    user,
                                    pattern,
                                    Purchase.PaymentStatus.PENDING
                            )
                            .orElseGet(Purchase::new);

            purchase.setBuyer(user);
            purchase.setPattern(pattern);
            purchase.setAmount(pattern.getPrice());
            purchase.setPaymentStatus(
                    Purchase.PaymentStatus.PENDING
            );

            purchaseRepository.save(purchase);

            purchaseIds.add(
                    purchase.getId()
            );
        }

        if (purchaseIds.isEmpty()) {

            redirectAttributes.addFlashAttribute(
                    "wishlistMessage",
                    "There are no unpaid patterns ready to purchase."
            );

            return "redirect:/wishlist";
        }

        session.setAttribute(
                "wishlistCheckoutPurchaseIds",
                purchaseIds
        );

        return "redirect:/payment/wishlist";
    }

    private User findLoggedInUser(
            Authentication authentication) {

        return userRepository
                .findByUsername(authentication.getName())
                .orElseThrow(() ->
                        new IllegalStateException(
                                "Logged-in user not found"
                        )
                );
    }
}