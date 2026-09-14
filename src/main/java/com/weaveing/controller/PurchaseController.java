package com.weaveing.controller;

import com.weaveing.entity.Pattern;
import com.weaveing.entity.Purchase;
import com.weaveing.entity.User;
import com.weaveing.repository.PatternRepository;
import com.weaveing.repository.PurchaseRepository;
import com.weaveing.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class PurchaseController {

    private final PatternRepository patternRepository;
    private final UserRepository userRepository;
    private final PurchaseRepository purchaseRepository;

    public PurchaseController(
            PatternRepository patternRepository,
            UserRepository userRepository,
            PurchaseRepository purchaseRepository) {

        this.patternRepository = patternRepository;
        this.userRepository = userRepository;
        this.purchaseRepository = purchaseRepository;
    }

    @PostMapping("/purchase/{patternId}")
    public String createPurchase(
            @PathVariable Long patternId,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {

        Pattern pattern = patternRepository.findById(patternId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Pattern not found")
                );

        if (pattern.getApprovalStatus() != Pattern.ApprovalStatus.APPROVED) {
            return "redirect:/home";
        }

        if (pattern.isFree()) {
            redirectAttributes.addFlashAttribute(
                    "purchaseMessage",
                    "This pattern is free and does not require a purchase."
            );

            return "redirect:/patterns/" + patternId;
        }

        User buyer = userRepository.findByUsername(authentication.getName())
                .orElseThrow(() ->
                        new IllegalStateException("Logged-in user not found")
                );

        boolean alreadyOwned =
                purchaseRepository.existsByBuyerAndPatternAndPaymentStatus(
                        buyer,
                        pattern,
                        Purchase.PaymentStatus.VERIFIED
                );

        if (alreadyOwned) {
            redirectAttributes.addFlashAttribute(
                    "purchaseMessage",
                    "You already own this pattern."
            );

            return "redirect:/patterns/" + patternId;
        }

        Purchase purchase =
                purchaseRepository
                        .findByBuyerAndPatternAndPaymentStatus(
                                buyer,
                                pattern,
                                Purchase.PaymentStatus.PENDING
                        )
                        .orElseGet(Purchase::new);

        purchase.setBuyer(buyer);
        purchase.setPattern(pattern);
        purchase.setAmount(pattern.getPrice());
        purchase.setPaymentStatus(Purchase.PaymentStatus.PENDING);

        purchaseRepository.save(purchase);

        return "redirect:/payment/" + purchase.getId();
    }
}