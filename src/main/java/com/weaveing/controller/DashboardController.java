package com.weaveing.controller;

import com.weaveing.entity.Pattern;
import com.weaveing.entity.Purchase;
import com.weaveing.entity.User;
import com.weaveing.repository.PatternRepository;
import com.weaveing.repository.PurchaseRepository;
import com.weaveing.repository.UserRepository;
import com.weaveing.repository.WishlistRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Controller
public class DashboardController {

    private final UserRepository userRepository;
    private final PatternRepository patternRepository;
    private final PurchaseRepository purchaseRepository;
    private final WishlistRepository wishlistRepository;
    private final SecurityContextRepository securityContextRepository =
            new HttpSessionSecurityContextRepository();

    public DashboardController(
            UserRepository userRepository,
            PatternRepository patternRepository,
            PurchaseRepository purchaseRepository,
            WishlistRepository wishlistRepository) {

        this.userRepository = userRepository;
        this.patternRepository = patternRepository;
        this.purchaseRepository = purchaseRepository;
        this.wishlistRepository = wishlistRepository;
    }

    @GetMapping("/dashboard")
    public String dashboard(
            Authentication authentication,
            Model model) {

        User user = getCurrentUser(authentication);

        if (user.isAdmin()) {
            return "redirect:/admin";
        }

        List<Pattern> myPatterns =
                patternRepository.findByCreator(user);

        List<Purchase> libraryPurchases =
                purchaseRepository.findByBuyerAndPaymentStatusOrderByPurchasedAtDesc(
                        user,
                        Purchase.PaymentStatus.VERIFIED
                );

        List<Purchase> sales = new ArrayList<>();

        long totalDownloads = 0;
        long totalSaves = 0;

        for (Pattern pattern : myPatterns) {

            List<Purchase> patternSales =
                    purchaseRepository.findByPatternAndPaymentStatus(
                            pattern,
                            Purchase.PaymentStatus.VERIFIED
                    );

            sales.addAll(patternSales);

            pattern.setPurchaseCount(patternSales.size());

            totalDownloads += pattern.getDownloads();
            totalSaves += wishlistRepository.countByPattern(pattern);
        }

        sales.sort(
                Comparator.comparing(
                        Purchase::getPurchasedAt,
                        Comparator.nullsLast(
                                Comparator.reverseOrder()
                        )
                )
        );

        double totalEarnings =
                sales.stream()
                        .mapToDouble(Purchase::getAmount)
                        .sum();

        long approvedPatterns =
                myPatterns.stream()
                        .filter(pattern ->
                                pattern.getApprovalStatus()
                                        == Pattern.ApprovalStatus.APPROVED)
                        .count();

        long pendingPatterns =
                myPatterns.stream()
                        .filter(pattern ->
                                pattern.getApprovalStatus()
                                        == Pattern.ApprovalStatus.PENDING)
                        .count();

        long rejectedPatterns =
                myPatterns.stream()
                        .filter(pattern ->
                                pattern.getApprovalStatus()
                                        == Pattern.ApprovalStatus.REJECTED)
                        .count();

        model.addAttribute("user", user);
        model.addAttribute("myPatterns", myPatterns);
        model.addAttribute("libraryPurchases", libraryPurchases);
        model.addAttribute(
                "wishlistItems",
                wishlistRepository.findByUserOrderByCreatedAtDesc(user)
        );
        model.addAttribute("sales", sales);
        model.addAttribute("totalEarnings", totalEarnings);
        model.addAttribute("patternsSold", sales.size());
        model.addAttribute("totalDownloads", totalDownloads);
        model.addAttribute("totalSaves", totalSaves);
        model.addAttribute("approvedPatterns", approvedPatterns);
        model.addAttribute("pendingPatterns", pendingPatterns);
        model.addAttribute("rejectedPatterns", rejectedPatterns);

        return "dashboard";
    }

    @GetMapping("/profile")
    public String profile(
            Authentication authentication,
            Model model,
            @RequestParam(
                    value = "updated",
                    required = false
            ) String updated) {

        User user = getCurrentUser(authentication);

        if (user.isAdmin()) {
            return "redirect:/admin";
        }

        addProfileData(user, model);

        if ("true".equals(updated)) {
            model.addAttribute(
                    "profileUpdated",
                    "Your profile has been updated."
            );
        }

        return "profile";
    }

    @GetMapping("/profile/edit")
    public String editProfilePage(
            Authentication authentication,
            Model model,
            @RequestParam(
                    value = "updated",
                    required = false
            ) String updated) {

        User user = getCurrentUser(authentication);

        if (user.isAdmin()) {
            return "redirect:/admin";
        }

        model.addAttribute("user", user);

        if ("true".equals(updated)) {
            model.addAttribute(
                    "profileUpdated",
                    "Your profile has been updated."
            );
        }

        return "profile-edit";
    }

    @PostMapping("/profile/edit")
    public String editProfile(
            Authentication authentication,
            HttpServletRequest request,
            HttpServletResponse response,
            Model model,
            @RequestParam("name") String name,
            @RequestParam("username") String username,
            @RequestParam("bio") String bio,
            @RequestParam("profilePicture") MultipartFile profilePicture) {

        User user = getCurrentUser(authentication);

        if (user.isAdmin()) {
            return "redirect:/admin";
        }

        name = name == null ? "" : name.trim();
        username = username == null ? "" : username.trim();
        bio = bio == null ? "" : bio.trim();

        if (name.isBlank()) {
            addProfileData(user, model);
            model.addAttribute(
                    "profileError",
                    "Please enter your name."
            );
            return "profile-edit";
        }

        if (username.isBlank()) {
            addProfileData(user, model);
            model.addAttribute(
                    "profileError",
                    "Please enter a username."
            );
            return "profile-edit";
        }

        if (username.length() < 3 || username.length() > 30) {
            addProfileData(user, model);
            model.addAttribute(
                    "profileError",
                    "Your username must be between 3 and 30 characters."
            );
            return "profile-edit";
        }

        if (!username.matches("[A-Za-z0-9._]+")) {
            addProfileData(user, model);
            model.addAttribute(
                    "profileError",
                    "Username can only contain letters, numbers, dots, and underscores."
            );
            return "profile-edit";
        }

        if (!username.equals(user.getUsername()) &&
                userRepository.existsByUsername(username)) {

            addProfileData(user, model);
            model.addAttribute(
                    "profileError",
                    "That username is already taken. Please choose another one."
            );
            return "profile-edit";
        }

        if (bio.length() > 500) {
            addProfileData(user, model);
            model.addAttribute(
                    "profileError",
                    "Your bio can be up to 500 characters."
            );
            return "profile-edit";
        }

        try {

            if (profilePicture != null &&
                    !profilePicture.isEmpty()) {

                saveProfilePicture(user, profilePicture);
            }

            user.setName(name);
            user.setUsername(username);
            user.setBio(bio.isBlank() ? null : bio);

            userRepository.save(user);

            if (!username.equals(authentication.getName())) {

                Authentication updatedAuthentication =
                        new UsernamePasswordAuthenticationToken(
                                user.getUsername(),
                                null,
                                authentication.getAuthorities()
                        );

                SecurityContextHolder
                        .getContext()
                        .setAuthentication(updatedAuthentication);

                securityContextRepository.saveContext(
                        SecurityContextHolder.getContext(),
                        request,
                        response
                );
            }

        } catch (IOException e) {

            addProfileData(user, model);
            model.addAttribute(
                    "profileError",
                    "We could not save your profile picture. Please try again."
            );
            return "profile-edit";
        }

        return "redirect:/profile/edit?updated=true";
    }

    @PostMapping({
            "/profile/profile-picture",
            "/dashboard/profile-picture"
    })
    public String uploadProfilePicture(
            Authentication authentication,
            @RequestParam("profilePicture")
            MultipartFile profilePicture) {

        User user = getCurrentUser(authentication);

        if (profilePicture == null ||
                profilePicture.isEmpty()) {

            return "redirect:/profile";
        }

        try {
            saveProfilePicture(user, profilePicture);
        } catch (IOException e) {
            throw new IllegalStateException(
                    "Could not save profile picture",
                    e
            );
        }

        return "redirect:/profile";
    }

    private void saveProfilePicture(
            User user,
            MultipartFile profilePicture)
            throws IOException {

        String contentType =
                profilePicture.getContentType();

        if (contentType == null ||
                (!contentType.equalsIgnoreCase("image/jpeg")
                        && !contentType.equalsIgnoreCase("image/png")
                        && !contentType.equalsIgnoreCase("image/webp"))) {

            throw new IOException("Unsupported image type");
        }

        String extension;

        if (contentType.equalsIgnoreCase("image/png")) {
            extension = ".png";
        } else if (contentType.equalsIgnoreCase("image/webp")) {
            extension = ".webp";
        } else {
            extension = ".jpg";
        }

        Path uploadDirectory =
                Paths.get(
                                "uploads",
                                "profile-pictures"
                        )
                        .toAbsolutePath()
                        .normalize();

        Files.createDirectories(uploadDirectory);

        String filename =
                "profile-"
                        + user.getId()
                        + "-"
                        + UUID.randomUUID()
                        + extension;

        Path destination =
                uploadDirectory
                        .resolve(filename)
                        .normalize();

        if (!destination.startsWith(uploadDirectory)) {
            throw new IOException("Invalid profile picture path");
        }

        Files.copy(
                profilePicture.getInputStream(),
                destination,
                StandardCopyOption.REPLACE_EXISTING
        );

        deleteOldProfilePicture(
                user.getProfileImagePath(),
                uploadDirectory
        );

        user.setProfileImagePath(
                "/uploads/profile-pictures/"
                        + filename
        );
    }

    private void deleteOldProfilePicture(
            String oldImagePath,
            Path uploadDirectory)
            throws IOException {

        if (oldImagePath == null ||
                !oldImagePath.startsWith(
                        "/uploads/profile-pictures/"
                )) {
            return;
        }

        String oldFilename =
                oldImagePath.substring(
                        "/uploads/profile-pictures/".length()
                );

        Path oldFile =
                uploadDirectory
                        .resolve(oldFilename)
                        .normalize();

        if (oldFile.startsWith(uploadDirectory) &&
                Files.exists(oldFile)) {

            Files.deleteIfExists(oldFile);
        }
    }

    private void addProfileData(
            User user,
            Model model) {

        List<Pattern> myPatterns =
                patternRepository.findByCreator(user);

        List<Purchase> libraryPurchases =
                purchaseRepository.findByBuyerAndPaymentStatusOrderByPurchasedAtDesc(
                        user,
                        Purchase.PaymentStatus.VERIFIED
                );

        model.addAttribute("user", user);
        model.addAttribute("myPatterns", myPatterns);
        model.addAttribute("libraryPurchases", libraryPurchases);
        model.addAttribute(
                "wishlistItems",
                wishlistRepository.findByUserOrderByCreatedAtDesc(user)
        );
    }

    private User getCurrentUser(
            Authentication authentication) {

        return userRepository.findByUsername(
                authentication.getName()
        ).orElseThrow(() ->
                new IllegalStateException(
                        "Logged-in user not found"
                )
        );
    }

}
