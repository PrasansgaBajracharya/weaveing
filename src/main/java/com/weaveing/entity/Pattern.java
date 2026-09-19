// weave.ing pattern entity.

package com.weaveing.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "patterns")
public class Pattern {

    public static final List<String> CATEGORIES = List.of(
            "Amigurumi & Toys",
            "Bags & Pouches",
            "Wearables",
            "Home & Decor",
            "Accessories",
            "Flowers & Plants",
            "Seasonal & Gifts"
    );

    public enum ApprovalStatus {
        PENDING,
        APPROVED,
        REJECTED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @ManyToOne
    @JoinColumn(name = "creator_id", nullable = false)
    private User creator;

    private String category;

    private String difficulty;

    private boolean free = false;

    private double price = 0.0;

    private double originalPrice = 0.0;

    private double discountPercent = 0.0;

    private LocalDateTime removedAt;

    private String imagePath;

    @Lob
    @Column(columnDefinition = "LONGBLOB")
    private byte[] imageData;

    private String imageContentType;

    private Integer imagePositionX = 50;

    private Integer imagePositionY = 50;

    private String filePath;

    private long likes = 0;

    private long downloads = 0;

    @Enumerated(EnumType.STRING)
    private ApprovalStatus approvalStatus =
            ApprovalStatus.PENDING;

    @Column(columnDefinition = "TEXT")
    private String rejectionReason;

    private LocalDateTime submittedAt;

    private LocalDateTime reviewedAt;

    @Transient
    private long purchaseCount = 0;

    public Pattern() {
        this.submittedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public User getCreator() {
        return creator;
    }

    public void setCreator(User creator) {
        this.creator = creator;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getCategoryLabel() {

        if (category == null || category.isBlank()) {
            return "Uncategorized";
        }

        return switch (category.trim().toLowerCase()) {

            case "amigurumi", "animals", "amigurumi & toys" ->
                    "Amigurumi & Toys";

            case "bags", "bag", "bags & pouches" ->
                    "Bags & Pouches";

            case "wearables", "wearable" ->
                    "Wearables";

            case "home decor", "home & decor", "home & décor" ->
                    "Home & Decor";

            case "accessories", "keychains", "accessory" ->
                    "Accessories";

            case "flowers", "flowers & plants" ->
                    "Flowers & Plants";

            case "seasonal", "seasonal & gifts" ->
                    "Seasonal & Gifts";

            default -> category.trim();
        };
    }

    public String getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(String difficulty) {
        this.difficulty = difficulty;
    }

    public boolean isFree() {
        return free;
    }

    public void setFree(boolean free) {
        this.free = free;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public double getOriginalPrice() {
        return originalPrice;
    }

    public void setOriginalPrice(double originalPrice) {
        this.originalPrice = originalPrice;
    }

    public double getDiscountPercent() {
        return discountPercent;
    }

    public void setDiscountPercent(double discountPercent) {
        this.discountPercent = discountPercent;
    }

    public LocalDateTime getRemovedAt() {
        return removedAt;
    }

    public void setRemovedAt(LocalDateTime removedAt) {
        this.removedAt = removedAt;
    }

    public String getImagePath() {
        if (imageData != null && imageData.length > 0 && id != null) {
            return "/patterns/" + id + "/image";
        }
        return imagePath;
    }

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }

    public byte[] getImageData() {
        return imageData;
    }

    public void setImageData(byte[] imageData) {
        this.imageData = imageData;
    }

    public String getImageContentType() {
        return imageContentType;
    }

    public void setImageContentType(String imageContentType) {
        this.imageContentType = imageContentType;
    }

    public int getImagePositionX() {
        return imagePositionX == null ? 50 : imagePositionX;
    }

    public void setImagePositionX(int imagePositionX) {
        this.imagePositionX = Math.max(0, Math.min(100, imagePositionX));
    }

    public int getImagePositionY() {
        return imagePositionY == null ? 50 : imagePositionY;
    }

    public void setImagePositionY(int imagePositionY) {
        this.imagePositionY = Math.max(0, Math.min(100, imagePositionY));
    }

    public String getImageObjectPosition() {
        return getImagePositionX() + "% " + getImagePositionY() + "%";
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public long getLikes() {
        return likes;
    }

    public void setLikes(long likes) {
        this.likes = likes;
    }

    public long getDownloads() {
        return downloads;
    }

    public void setDownloads(long downloads) {
        this.downloads = downloads;
    }

    public ApprovalStatus getApprovalStatus() {
        return approvalStatus;
    }

    public void setApprovalStatus(
            ApprovalStatus approvalStatus) {

        this.approvalStatus = approvalStatus;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(
            String rejectionReason) {

        this.rejectionReason = rejectionReason;
    }

    public LocalDateTime getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(
            LocalDateTime submittedAt) {

        this.submittedAt = submittedAt;
    }

    public LocalDateTime getReviewedAt() {
        return reviewedAt;
    }

    public void setReviewedAt(
            LocalDateTime reviewedAt) {

        this.reviewedAt = reviewedAt;
    }

    public long getPurchaseCount() {
        return purchaseCount;
    }

    public void setPurchaseCount(
            long purchaseCount) {

        this.purchaseCount = purchaseCount;
    }
}