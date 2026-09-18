package com.weaveing.controller;

import com.weaveing.entity.Pattern;
import com.weaveing.entity.Purchase;
import com.weaveing.entity.Review;
import com.weaveing.entity.User;
import com.weaveing.repository.PatternRepository;
import com.weaveing.repository.PurchaseRepository;
import com.weaveing.repository.ReviewRepository;
import com.weaveing.repository.UserRepository;
import com.weaveing.repository.WishlistRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import com.weaveing.service.EmailService;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.UUID;

@Controller
public class PatternController {

    private final PatternRepository patternRepository;
    private final UserRepository userRepository;
    private final PurchaseRepository purchaseRepository;
    private final ReviewRepository reviewRepository;
    private final EmailService emailService;
    private final WishlistRepository wishlistRepository;

    public PatternController(
            PatternRepository patternRepository,
            UserRepository userRepository,
            PurchaseRepository purchaseRepository,
            ReviewRepository reviewRepository,
            EmailService emailService,
            WishlistRepository wishlistRepository) {

        this.patternRepository = patternRepository;
        this.userRepository = userRepository;
        this.purchaseRepository = purchaseRepository;
        this.reviewRepository = reviewRepository;
        this.emailService = emailService;
        this.wishlistRepository = wishlistRepository;
    }


    @GetMapping("/patterns/new")
    public String newPattern(Model model) {
        model.addAttribute("categories", Pattern.CATEGORIES);
        return "pattern-form";
    }

    @PostMapping("/patterns/new")
    public String submitPattern(
            @RequestParam("title") String title,
            @RequestParam("description") String description,
            @RequestParam("category") String category,
            @RequestParam("difficulty") String difficulty,
            @RequestParam("priceType") String priceType,
            @RequestParam("price") double price,
            @RequestParam("image") MultipartFile image,
            @RequestParam("patternFile") MultipartFile patternFile,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {

        try {
            User user = getCurrentUser(authentication);

            if (title == null || title.isBlank()
                    || description == null || description.isBlank()
                    || category == null || category.isBlank()
                    || difficulty == null || difficulty.isBlank()) {

                redirectAttributes.addFlashAttribute(
                        "patternError",
                        "Please complete all required pattern details."
                );

                return "redirect:/patterns/new";
            }

            if (!Pattern.CATEGORIES.contains(category.trim())) {

                redirectAttributes.addFlashAttribute(
                        "patternError",
                        "Please select a valid pattern category."
                );

                return "redirect:/patterns/new";
            }

            if (image == null || image.isEmpty()) {

                redirectAttributes.addFlashAttribute(
                        "patternError",
                        "Please upload a pattern cover image."
                );

                return "redirect:/patterns/new";
            }

            if (patternFile == null || patternFile.isEmpty()) {

                redirectAttributes.addFlashAttribute(
                        "patternError",
                        "Please upload your digital pattern file."
                );

                return "redirect:/patterns/new";
            }

            String imageOriginalName =
                    image.getOriginalFilename();

            String patternOriginalName =
                    patternFile.getOriginalFilename();

            if (imageOriginalName == null ||
                    !isAllowedImage(imageOriginalName)) {

                redirectAttributes.addFlashAttribute(
                        "patternError",
                        "Cover image must be PNG, JPG, JPEG, or WEBP."
                );

                return "redirect:/patterns/new";
            }

            if (patternOriginalName == null ||
                    !patternOriginalName
                            .toLowerCase()
                            .endsWith(".pdf")) {

                redirectAttributes.addFlashAttribute(
                        "patternError",
                        "The digital pattern file must be a PDF."
                );

                return "redirect:/patterns/new";
            }

            boolean isFree =
                    "free".equalsIgnoreCase(priceType);

            if (isFree) {
                price = 0.0;
            }

            if (!isFree && price <= 0) {

                redirectAttributes.addFlashAttribute(
                        "patternError",
                        "Please enter a valid price for a paid pattern."
                );

                return "redirect:/patterns/new";
            }

            Path imageDirectory =
                    Paths.get(
                                    "uploads",
                                    "pattern-images"
                            )
                            .toAbsolutePath()
                            .normalize();

            Path patternDirectory =
                    Paths.get(
                                    "uploads",
                                    "private-patterns"
                            )
                            .toAbsolutePath()
                            .normalize();

            Files.createDirectories(imageDirectory);
            Files.createDirectories(patternDirectory);

            String imageExtension =
                    getFileExtension(imageOriginalName);

            String imageFilename =
                    UUID.randomUUID()
                            .toString()
                            + imageExtension;

            String patternFilename =
                    UUID.randomUUID()
                            .toString()
                            + ".pdf";

            Path imagePath =
                    imageDirectory
                            .resolve(imageFilename)
                            .normalize();

            Path patternPath =
                    patternDirectory
                            .resolve(patternFilename)
                            .normalize();

            if (!imagePath.startsWith(imageDirectory)
                    || !patternPath.startsWith(patternDirectory)) {

                redirectAttributes.addFlashAttribute(
                        "patternError",
                        "Unable to save uploaded files."
                );

                return "redirect:/patterns/new";
            }

            Files.copy(
                    image.getInputStream(),
                    imagePath,
                    StandardCopyOption.REPLACE_EXISTING
            );

            Files.copy(
                    patternFile.getInputStream(),
                    patternPath,
                    StandardCopyOption.REPLACE_EXISTING
            );

            Pattern pattern =
                    new Pattern();

            pattern.setTitle(
                    title.trim()
            );

            pattern.setDescription(
                    description.trim()
            );

            pattern.setCategory(
                    category.trim()
            );

            pattern.setDifficulty(
                    difficulty.trim()
            );

            pattern.setCreator(user);

            pattern.setFree(isFree);

            pattern.setPrice(price);
            pattern.setOriginalPrice(isFree ? 0.0 : price);
            pattern.setDiscountPercent(0.0);

            pattern.setImagePath(
                    "/uploads/pattern-images/"
                            + imageFilename
            );

            pattern.setFilePath(
                    "/uploads/private-patterns/"
                            + patternFilename
            );

            pattern.setApprovalStatus(
                    Pattern.ApprovalStatus.PENDING
            );

            pattern.setSubmittedAt(
                    LocalDateTime.now()
            );

            pattern.setLikes(0);
            pattern.setDownloads(0);

            patternRepository.save(pattern);
            try {
                emailService.sendPendingPatternAdminNotification(pattern);
            } catch (Exception ignored) {
            }

            redirectAttributes.addFlashAttribute(
                    "patternSuccess",
                    "Your pattern has been submitted successfully and is waiting for review."
            );

            return "redirect:/dashboard";

        } catch (IOException e) {

            redirectAttributes.addFlashAttribute(
                    "patternError",
                    "There was a problem uploading your files. Please try again."
            );

            return "redirect:/patterns/new";
        }
    }

    @GetMapping("/patterns/{id}/edit")
    public String editPatternPage(
            @PathVariable Long id,
            Authentication authentication,
            Model model) {

        User user = getCurrentUser(authentication);
        Pattern pattern = patternRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Pattern not found"));

        if (!isPatternOwner(pattern, user)) {
            return "redirect:/profile#patterns";
        }

        model.addAttribute("pattern", pattern);
        model.addAttribute("categories", Pattern.CATEGORIES);
        model.addAttribute(
                "editOriginalPrice",
                pattern.getOriginalPrice() > 0
                        ? pattern.getOriginalPrice()
                        : pattern.getPrice()
        );

        return "pattern-edit";
    }

    @PostMapping("/patterns/{id}/edit")
    public String editPattern(
            @PathVariable Long id,
            @RequestParam("title") String title,
            @RequestParam("description") String description,
            @RequestParam("category") String category,
            @RequestParam("difficulty") String difficulty,
            @RequestParam("priceType") String priceType,
            @RequestParam("originalPrice") double originalPrice,
            @RequestParam("discountPercent") double discountPercent,
            @RequestParam(value = "image", required = false) MultipartFile image,
            @RequestParam(value = "patternFile", required = false) MultipartFile patternFile,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {

        try {
            User user = getCurrentUser(authentication);
            Pattern pattern = patternRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Pattern not found"));

            if (!isPatternOwner(pattern, user)) {
                return "redirect:/profile#patterns";
            }

            title = title == null ? "" : title.trim();
            description = description == null ? "" : description.trim();
            category = category == null ? "" : category.trim();
            difficulty = difficulty == null ? "" : difficulty.trim();

            if (title.isBlank() || description.isBlank()
                    || category.isBlank() || difficulty.isBlank()
                    || !Pattern.CATEGORIES.contains(category)) {
                redirectAttributes.addFlashAttribute(
                        "patternError",
                        "Please complete all required pattern details."
                );
                return "redirect:/patterns/" + id + "/edit";
            }

            boolean isFree = "free".equalsIgnoreCase(priceType);

            if (isFree) {
                originalPrice = 0.0;
                discountPercent = 0.0;
            } else {
                if (originalPrice <= 0) {
                    redirectAttributes.addFlashAttribute(
                            "patternError",
                            "Please enter a valid original price."
                    );
                    return "redirect:/patterns/" + id + "/edit";
                }

                if (discountPercent < 0 || discountPercent > 90) {
                    redirectAttributes.addFlashAttribute(
                            "patternError",
                            "Discount must be between 0% and 90%."
                    );
                    return "redirect:/patterns/" + id + "/edit";
                }
            }

            double finalPrice = isFree
                    ? 0.0
                    : Math.round(originalPrice * (1.0 - discountPercent / 100.0) * 100.0) / 100.0;

            if (!isFree && finalPrice <= 0) {
                redirectAttributes.addFlashAttribute(
                        "patternError",
                        "The discounted price must be greater than Rs 0."
                );
                return "redirect:/patterns/" + id + "/edit";
            }

            boolean contentChanged =
                    !safeEquals(pattern.getTitle(), title)
                    || !safeEquals(pattern.getDescription(), description)
                    || !safeEquals(pattern.getCategory(), category)
                    || !safeEquals(pattern.getDifficulty(), difficulty)
                    || (image != null && !image.isEmpty())
                    || (patternFile != null && !patternFile.isEmpty());

            if (image != null && !image.isEmpty()) {
                String originalName = image.getOriginalFilename();

                if (originalName == null || !isAllowedImage(originalName)) {
                    redirectAttributes.addFlashAttribute(
                            "patternError",
                            "Cover image must be PNG, JPG, JPEG, or WEBP."
                    );
                    return "redirect:/patterns/" + id + "/edit";
                }

                Path imageDirectory = Paths.get("uploads", "pattern-images")
                        .toAbsolutePath().normalize();
                Files.createDirectories(imageDirectory);

                String filename = UUID.randomUUID() + getFileExtension(originalName);
                Path imagePath = imageDirectory.resolve(filename).normalize();

                if (!imagePath.startsWith(imageDirectory)) {
                    throw new IOException("Invalid image path");
                }

                Files.copy(
                        image.getInputStream(),
                        imagePath,
                        StandardCopyOption.REPLACE_EXISTING
                );

                pattern.setImagePath("/uploads/pattern-images/" + filename);
            }

            if (patternFile != null && !patternFile.isEmpty()) {
                String originalName = patternFile.getOriginalFilename();

                if (originalName == null || !originalName.toLowerCase().endsWith(".pdf")) {
                    redirectAttributes.addFlashAttribute(
                            "patternError",
                            "The digital pattern file must be a PDF."
                    );
                    return "redirect:/patterns/" + id + "/edit";
                }

                Path patternDirectory = Paths.get("uploads", "private-patterns")
                        .toAbsolutePath().normalize();
                Files.createDirectories(patternDirectory);

                String filename = UUID.randomUUID() + ".pdf";
                Path patternPath = patternDirectory.resolve(filename).normalize();

                if (!patternPath.startsWith(patternDirectory)) {
                    throw new IOException("Invalid pattern path");
                }

                Files.copy(
                        patternFile.getInputStream(),
                        patternPath,
                        StandardCopyOption.REPLACE_EXISTING
                );

                pattern.setFilePath("/uploads/private-patterns/" + filename);
            }

            pattern.setTitle(title);
            pattern.setDescription(description);
            pattern.setCategory(category);
            pattern.setDifficulty(difficulty);
            pattern.setFree(isFree);
            pattern.setOriginalPrice(originalPrice);
            pattern.setDiscountPercent(discountPercent);
            pattern.setPrice(finalPrice);

            if (contentChanged &&
                    pattern.getApprovalStatus() != Pattern.ApprovalStatus.PENDING) {
                pattern.setApprovalStatus(Pattern.ApprovalStatus.PENDING);
                pattern.setRejectionReason(null);
                pattern.setReviewedAt(null);
                pattern.setSubmittedAt(LocalDateTime.now());
            }

            patternRepository.save(pattern);

            redirectAttributes.addFlashAttribute(
                    "patternSuccess",
                    contentChanged
                            ? "Pattern updated and sent for admin review."
                            : "Pattern pricing updated successfully."
            );

            return "redirect:/profile#patterns";

        } catch (IOException e) {
            redirectAttributes.addFlashAttribute(
                    "patternError",
                    "There was a problem updating the pattern. Please try again."
            );
            return "redirect:/patterns/" + id + "/edit";
        }
    }

    @PostMapping("/patterns/{id}/delete")
    public String removePattern(
            @PathVariable Long id,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {

        User user = getCurrentUser(authentication);
        Pattern pattern = patternRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Pattern not found"));

        if (!isPatternOwner(pattern, user)) {
            return "redirect:/profile#patterns";
        }

        pattern.setRemovedAt(LocalDateTime.now());
        patternRepository.save(pattern);
        wishlistRepository.deleteByPattern(pattern);

        redirectAttributes.addFlashAttribute(
                "patternSuccess",
                "Pattern removed from the marketplace. Existing purchases remain available."
        );

        return "redirect:/profile#patterns";
    }

    @PostMapping("/patterns/{id}/restore")
    public String restorePattern(
            @PathVariable Long id,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {

        User user = getCurrentUser(authentication);
        Pattern pattern = patternRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Pattern not found"));

        if (!isPatternOwner(pattern, user)) {
            return "redirect:/profile#patterns";
        }

        pattern.setRemovedAt(null);
        pattern.setApprovalStatus(Pattern.ApprovalStatus.PENDING);
        pattern.setSubmittedAt(LocalDateTime.now());
        pattern.setReviewedAt(null);
        pattern.setRejectionReason(null);
        patternRepository.save(pattern);

        try {
            emailService.sendPendingPatternAdminNotification(pattern);
        } catch (Exception ignored) {
        }

        redirectAttributes.addFlashAttribute(
                "patternSuccess",
                "Pattern restored and sent for admin review."
        );

        return "redirect:/profile#patterns";
    }

    private boolean isPatternOwner(Pattern pattern, User user) {
        return pattern.getCreator() != null
                && user != null
                && pattern.getCreator().getId().equals(user.getId());
    }

    private boolean safeEquals(String first, String second) {
        return first == null ? second == null : first.equals(second);
    }

    @GetMapping("/patterns/{id}")
    public String patternDetails(
            @PathVariable Long id,
            Authentication authentication,
            Model model) {

        Pattern pattern = patternRepository.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Pattern not found"
                        )
                );

        User user = null;

        if (authentication != null &&
                authentication.isAuthenticated()) {

            user = userRepository
                    .findByUsername(authentication.getName())
                    .orElse(null);
        }

        boolean ownsPattern = pattern.isFree();

        Purchase pendingPurchase = null;

        boolean verifiedPurchase = false;

        if (!pattern.isFree() && user != null) {
            verifiedPurchase = purchaseRepository
                    .existsByBuyerAndPatternAndPaymentStatus(
                            user,
                            pattern,
                            Purchase.PaymentStatus.VERIFIED
                    );
        }

        if (pattern.getApprovalStatus()
                != Pattern.ApprovalStatus.APPROVED) {

            return "redirect:/home";
        }

        if (pattern.getRemovedAt() != null && !verifiedPurchase) {
            return "redirect:/home";
        }

        if (!pattern.isFree() && user != null) {

            ownsPattern = verifiedPurchase;

            pendingPurchase =
                    purchaseRepository
                            .findByBuyerAndPatternAndPaymentStatus(
                                    user,
                                    pattern,
                                    Purchase.PaymentStatus.PENDING
                            )
                            .orElse(null);
        }

        java.util.List<Pattern> relatedPatterns =
                patternRepository
                        .findTop4ByApprovalStatusAndRemovedAtIsNullAndCategoryAndIdNotOrderBySubmittedAtDesc(
                                Pattern.ApprovalStatus.APPROVED,
                                pattern.getCategory(),
                                pattern.getId()
                        );

        long purchaseCount =
                purchaseRepository.countByPatternAndPaymentStatus(
                        pattern,
                        Purchase.PaymentStatus.VERIFIED
                );

        pattern.setPurchaseCount(purchaseCount);

        boolean hasPendingPurchase =
                !pattern.isFree()
                        && !ownsPattern
                        && pendingPurchase != null;

        model.addAttribute(
                "pattern",
                pattern
        );

        model.addAttribute(
                "ownsPattern",
                ownsPattern
        );

        model.addAttribute(
                "hasPendingPurchase",
                hasPendingPurchase
        );

        model.addAttribute(
                "pendingPurchase",
                pendingPurchase
        );

        model.addAttribute(
                "purchaseCreated",
                false
        );

        model.addAttribute(
                "isAuthenticated",
                user != null
        );

        model.addAttribute(
                "relatedPatterns",
                relatedPatterns
        );

        java.util.List<Review> reviews =
                reviewRepository.findByPatternOrderByCreatedAtDesc(pattern);

        Double averageRating =
                reviewRepository.getAverageRatingByPattern(pattern);

        Review currentUserReview = null;
        boolean canReview = false;

        if (user != null &&
                pattern.getCreator() != null &&
                !pattern.getCreator().getId().equals(user.getId())) {

            currentUserReview =
                    reviewRepository
                            .findByReviewerAndPattern(user, pattern)
                            .orElse(null);

            canReview =
                    purchaseRepository
                            .existsByBuyerAndPatternAndPaymentStatus(
                                    user,
                                    pattern,
                                    Purchase.PaymentStatus.VERIFIED
                            );
        }

        model.addAttribute("reviews", reviews);
        model.addAttribute(
                "averageRating",
                averageRating == null ? 0.0 : averageRating
        );
        model.addAttribute(
                "reviewCount",
                reviews.size()
        );
        model.addAttribute("currentUserReview", currentUserReview);
        model.addAttribute("canReview", canReview);

        return "pattern-details";
    }

    @GetMapping("/patterns/{id}/view")
    public ResponseEntity<Resource> viewPattern(
            @PathVariable Long id,
            Authentication authentication) {

        Pattern pattern = patternRepository.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Pattern not found"
                        )
                );

        User user = getCurrentUser(authentication);

        if (!canAccessPattern(pattern, user)) {
            return ResponseEntity.status(403).build();
        }

        Path filePath = resolvePatternFile(pattern);

        if (filePath == null ||
                !Files.exists(filePath) ||
                !Files.isRegularFile(filePath)) {

            return ResponseEntity.notFound().build();
        }

        Resource resource =
                new FileSystemResource(filePath);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"" +
                                safeFilename(pattern) +
                                "\""
                )
                .body(resource);
    }

    @GetMapping("/patterns/{id}/download")
    public ResponseEntity<Resource> downloadPattern(
            @PathVariable Long id,
            Authentication authentication) {

        Pattern pattern = patternRepository.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Pattern not found"
                        )
                );

        User user = getCurrentUser(authentication);

        if (!canAccessPattern(pattern, user)) {
            return ResponseEntity.status(403).build();
        }

        Path filePath = resolvePatternFile(pattern);

        if (filePath == null ||
                !Files.exists(filePath) ||
                !Files.isRegularFile(filePath)) {

            return ResponseEntity.notFound().build();
        }

        pattern.setDownloads(
                pattern.getDownloads() + 1
        );

        patternRepository.save(pattern);

        Resource resource =
                new FileSystemResource(filePath);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" +
                                safeFilename(pattern) +
                                "\""
                )
                .body(resource);
    }

    private boolean canAccessPattern(
            Pattern pattern,
            User user) {

        if (pattern.getApprovalStatus()
                != Pattern.ApprovalStatus.APPROVED) {

            return false;
        }

        if (pattern.getRemovedAt() != null) {
            if (user == null) {
                return false;
            }

            if (pattern.getCreator() != null &&
                    pattern.getCreator().getId().equals(user.getId())) {
                return true;
            }

            return purchaseRepository
                    .existsByBuyerAndPatternAndPaymentStatus(
                            user,
                            pattern,
                            Purchase.PaymentStatus.VERIFIED
                    );
        }

        if (pattern.isFree()) {
            return true;
        }

        if (pattern.getCreator() != null &&
                pattern.getCreator().getId()
                        .equals(user.getId())) {

            return true;
        }

        return purchaseRepository
                .existsByBuyerAndPatternAndPaymentStatus(
                        user,
                        pattern,
                        Purchase.PaymentStatus.VERIFIED
                );
    }

    private Path resolvePatternFile(
            Pattern pattern) {

        String filePath =
                pattern.getFilePath();

        if (filePath == null ||
                filePath.isBlank()) {

            return null;
        }

        String filename =
                filePath;

        if (filename.startsWith(
                "/uploads/private-patterns/"
        )) {

            filename =
                    filename.substring(
                            "/uploads/private-patterns/"
                                    .length()
                    );

            Path privateDirectory =
                    Paths.get(
                                    "uploads",
                                    "private-patterns"
                            )
                            .toAbsolutePath()
                            .normalize();

            Path resolved =
                    privateDirectory
                            .resolve(filename)
                            .normalize();

            if (!resolved.startsWith(
                    privateDirectory
            )) {
                return null;
            }

            return resolved;
        }

        if (filename.startsWith(
                "/uploads/patterns/"
        )) {

            filename =
                    filename.substring(
                            "/uploads/patterns/"
                                    .length()
                    );

            Path patternDirectory =
                    Paths.get(
                                    "uploads",
                                    "patterns"
                            )
                            .toAbsolutePath()
                            .normalize();

            Path resolved =
                    patternDirectory
                            .resolve(filename)
                            .normalize();

            if (!resolved.startsWith(
                    patternDirectory
            )) {
                return null;
            }

            return resolved;
        }

        return null;
    }

    private String safeFilename(
            Pattern pattern) {

        String title =
                pattern.getTitle() == null
                        ? "pattern"
                        : pattern.getTitle();

        String filename =
                title
                        .replaceAll(
                                "[^a-zA-Z0-9-_ ]",
                                ""
                        )
                        .trim()
                        .replaceAll(
                                "\\s+",
                                "-"
                        );

        if (filename.isBlank()) {
            filename = "pattern";
        }

        return filename + ".pdf";
    }

    private User getCurrentUser(
            Authentication authentication) {

        return userRepository
                .findByUsername(
                        authentication.getName()
                )
                .orElseThrow(() ->
                        new IllegalStateException(
                                "Logged-in user not found"
                        )
                );
    }

    private boolean isAllowedImage(
            String filename) {

        String lower =
                filename.toLowerCase();

        return lower.endsWith(".png")
                || lower.endsWith(".jpg")
                || lower.endsWith(".jpeg")
                || lower.endsWith(".webp");
    }

    private String getFileExtension(
            String filename) {

        int dot =
                filename.lastIndexOf('.');

        if (dot < 0) {
            return "";
        }

        return filename.substring(dot)
                .toLowerCase();
    }
}