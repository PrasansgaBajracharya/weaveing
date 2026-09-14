package com.weaveing.controller;

import com.weaveing.entity.Purchase;
import com.weaveing.entity.User;
import com.weaveing.repository.PurchaseRepository;
import com.weaveing.repository.UserRepository;
import com.weaveing.service.EmailService;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Controller
public class PaymentController {

    private final PurchaseRepository purchaseRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;

    public PaymentController(
            PurchaseRepository purchaseRepository,
            UserRepository userRepository,
            EmailService emailService) {

        this.purchaseRepository = purchaseRepository;
        this.userRepository = userRepository;
        this.emailService = emailService;
    }

    @GetMapping("/payment/{purchaseId}")
    public String paymentPage(
            @PathVariable Long purchaseId,
            Authentication authentication,
            Model model,
            RedirectAttributes redirectAttributes) {

        Purchase purchase = purchaseRepository.findById(purchaseId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Purchase not found")
                );

        User currentUser = findLoggedInUser(authentication);

        if (!purchase.getBuyer().getId().equals(currentUser.getId())) {
            return "redirect:/home";
        }

        if (purchase.getPaymentStatus() == Purchase.PaymentStatus.VERIFIED) {

            redirectAttributes.addFlashAttribute(
                    "purchaseMessage",
                    "You already own this pattern."
            );

            return "redirect:/patterns/" +
                    purchase.getPattern().getId();
        }

        if (purchase.getPaymentStatus() != Purchase.PaymentStatus.PENDING) {

            redirectAttributes.addFlashAttribute(
                    "purchaseMessage",
                    "This payment order is no longer active. Please start a new purchase."
            );

            return "redirect:/patterns/" +
                    purchase.getPattern().getId();
        }

        model.addAttribute("purchase", purchase);
        model.addAttribute("pattern", purchase.getPattern());

        return "payment";
    }

    @PostMapping("/payment/{purchaseId}/process")
    public String processPayment(
            @PathVariable Long purchaseId,
            @RequestParam("paymentMethod") String paymentMethod,
            @RequestParam("result") String result,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {

        Purchase purchase = purchaseRepository.findById(purchaseId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Purchase not found")
                );

        User currentUser = findLoggedInUser(authentication);

        if (!purchase.getBuyer().getId().equals(currentUser.getId())) {
            return "redirect:/home";
        }

        if (purchase.getPaymentStatus() != Purchase.PaymentStatus.PENDING) {

            redirectAttributes.addFlashAttribute(
                    "purchaseMessage",
                    "This payment order is no longer active."
            );

            return "redirect:/patterns/" +
                    purchase.getPattern().getId();
        }

        if (!isSupportedPaymentMethod(paymentMethod)) {

            redirectAttributes.addFlashAttribute(
                    "purchaseMessage",
                    "Please select a payment method."
            );

            return "redirect:/payment/" + purchaseId;
        }

        if ("success".equalsIgnoreCase(result)) {

            String transactionId =
                    "WEAVE-" +
                            UUID.randomUUID()
                                    .toString()
                                    .replace("-", "")
                                    .substring(0, 12)
                                    .toUpperCase();

            purchase.setTransactionId(transactionId);

            purchase.setPaymentStatus(
                    Purchase.PaymentStatus.VERIFIED
            );

            purchaseRepository.save(purchase);

            emailService.sendPurchaseConfirmationEmail(
                    purchase
            );

            redirectAttributes.addFlashAttribute(
                    "purchaseMessage",
                    "Payment successful. Your pattern is now unlocked. Transaction ID: " +
                            transactionId
            );

            return "redirect:/patterns/" +
                    purchase.getPattern().getId();
        }

        purchase.setPaymentStatus(
                Purchase.PaymentStatus.FAILED
        );

        purchaseRepository.save(purchase);

        redirectAttributes.addFlashAttribute(
                "purchaseMessage",
                "Payment was not completed. You can try purchasing the pattern again."
        );

        return "redirect:/patterns/" +
                purchase.getPattern().getId();
    }

    @GetMapping("/payment/wishlist")
    public String wishlistPaymentPage(
            Authentication authentication,
            HttpSession session,
            Model model) {

        User currentUser = findLoggedInUser(authentication);

        Object storedIds =
                session.getAttribute("wishlistCheckoutPurchaseIds");

        if (!(storedIds instanceof List<?> ids) || ids.isEmpty()) {
            return "redirect:/wishlist";
        }

        List<Purchase> purchases = new ArrayList<>();

        for (Object idValue : ids) {

            if (!(idValue instanceof Long purchaseId)) {
                continue;
            }

            purchaseRepository.findById(purchaseId)
                    .filter(purchase ->
                            purchase.getBuyer().getId()
                                    .equals(currentUser.getId())
                                    && purchase.getPaymentStatus() ==
                                    Purchase.PaymentStatus.PENDING)
                    .ifPresent(purchases::add);
        }

        if (purchases.isEmpty()) {
            session.removeAttribute("wishlistCheckoutPurchaseIds");
            return "redirect:/wishlist";
        }

        double total =
                purchases.stream()
                        .mapToDouble(Purchase::getAmount)
                        .sum();

        model.addAttribute("wishlistPurchases", purchases);
        model.addAttribute("wishlistTotal", total);

        return "wishlist-payment";
    }

    @PostMapping("/payment/wishlist/process")
    public String processWishlistPayment(
            @RequestParam("paymentMethod") String paymentMethod,
            @RequestParam("result") String result,
            Authentication authentication,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        User currentUser = findLoggedInUser(authentication);

        if (!isSupportedPaymentMethod(paymentMethod)) {
            redirectAttributes.addFlashAttribute(
                    "wishlistMessage",
                    "Please select a payment method."
            );
            return "redirect:/payment/wishlist";
        }

        Object storedIds =
                session.getAttribute("wishlistCheckoutPurchaseIds");

        if (!(storedIds instanceof List<?> ids) || ids.isEmpty()) {
            return "redirect:/wishlist";
        }

        List<Purchase> purchases = new ArrayList<>();

        for (Object idValue : ids) {

            if (!(idValue instanceof Long purchaseId)) {
                continue;
            }

            purchaseRepository.findById(purchaseId)
                    .filter(purchase ->
                            purchase.getBuyer().getId()
                                    .equals(currentUser.getId())
                                    && purchase.getPaymentStatus() ==
                                    Purchase.PaymentStatus.PENDING)
                    .ifPresent(purchases::add);
        }

        if (purchases.isEmpty()) {
            session.removeAttribute("wishlistCheckoutPurchaseIds");
            return "redirect:/wishlist";
        }

        if ("success".equalsIgnoreCase(result)) {

            for (Purchase purchase : purchases) {

                String transactionId =
                        "WEAVE-" +
                                UUID.randomUUID()
                                        .toString()
                                        .replace("-", "")
                                        .substring(0, 12)
                                        .toUpperCase();

                purchase.setTransactionId(transactionId);
                purchase.setPaymentStatus(
                        Purchase.PaymentStatus.VERIFIED
                );

                purchaseRepository.save(purchase);

                emailService.sendPurchaseConfirmationEmail(
                        purchase
                );
            }

            session.removeAttribute("wishlistCheckoutPurchaseIds");

            redirectAttributes.addFlashAttribute(
                    "wishlistMessage",
                    purchases.size() +
                            " pattern" +
                            (purchases.size() == 1 ? "" : "s") +
                            " purchased successfully. They are now in your library."
            );

            return "redirect:/wishlist";
        }

        for (Purchase purchase : purchases) {
            purchase.setPaymentStatus(
                    Purchase.PaymentStatus.FAILED
            );
            purchaseRepository.save(purchase);
        }

        session.removeAttribute("wishlistCheckoutPurchaseIds");

        redirectAttributes.addFlashAttribute(
                "wishlistMessage",
                "Payment was not completed. You can return to your wishlist and try again."
        );

        return "redirect:/wishlist";
    }

    private User findLoggedInUser(
            Authentication authentication) {

        return userRepository.findByUsername(authentication.getName())
                .orElseThrow(() ->
                        new IllegalStateException(
                                "Logged-in user not found"
                        )
                );
    }

    private boolean isSupportedPaymentMethod(
            String paymentMethod) {

        return "esewa".equalsIgnoreCase(paymentMethod)
                || "khalti".equalsIgnoreCase(paymentMethod)
                || "fonepay".equalsIgnoreCase(paymentMethod)
                || "bank".equalsIgnoreCase(paymentMethod);
    }
}