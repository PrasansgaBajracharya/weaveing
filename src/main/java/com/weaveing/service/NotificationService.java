package com.weaveing.service;

import com.weaveing.entity.*;
import com.weaveing.repository.NotificationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final EmailService emailService;

    public NotificationService(
            NotificationRepository notificationRepository,
            EmailService emailService) {
        this.notificationRepository = notificationRepository;
        this.emailService = emailService;
    }

    @Transactional
    public Notification create(
            User user,
            String title,
            String message,
            String type) {

        Notification notification = saveNotification(
                user, title, message, type, "/notifications"
        );

        try {
            emailService.sendNotificationEmail(notification);
        } catch (Exception ignored) {
        }

        return notification;
    }

    @Transactional
    public void notifyPatternApproved(Pattern pattern) {

        if (pattern == null || pattern.getCreator() == null) {
            return;
        }

        saveNotification(
                pattern.getCreator(),
                "Pattern approved",
                "Your pattern “" + pattern.getTitle()
                        + "” has been approved and is now live on the marketplace.",
                "PATTERN_APPROVED",
                "/profile#patterns"
        );

        try {
            emailService.sendPatternApprovalEmail(pattern);
        } catch (Exception ignored) {
        }
    }

    @Transactional
    public void notifyPatternRejected(Pattern pattern) {

        if (pattern == null || pattern.getCreator() == null) {
            return;
        }

        String reason = pattern.getRejectionReason();

        if (reason == null || reason.isBlank()) {
            reason = "Pattern did not meet the review requirements.";
        }

        saveNotification(
                pattern.getCreator(),
                "Pattern rejected",
                "Your pattern “" + pattern.getTitle()
                        + "” was not approved. Reason: " + reason,
                "PATTERN_REJECTED",
                "/profile#patterns"
        );

        try {
            emailService.sendPatternRejectionEmail(pattern);
        } catch (Exception ignored) {
        }
    }

    @Transactional
    public void notifyPurchaseConfirmed(Purchase purchase) {

        if (purchase == null
                || purchase.getBuyer() == null
                || purchase.getPattern() == null) {
            return;
        }

        saveNotification(
                purchase.getBuyer(),
                "Purchase successful",
                "Your pattern “" + purchase.getPattern().getTitle()
                        + "” is now unlocked and available in your library.",
                "PURCHASE_CONFIRMED",
                "/profile#patterns"
        );

        try {
            emailService.sendPurchaseConfirmationEmail(purchase);
        } catch (Exception ignored) {
        }
    }

    @Transactional
    public void notifyNewVacancyApplication(Application application) {

        if (application == null ||
                application.getVacancy() == null ||
                application.getVacancy().getPostedBy() == null) {
            return;
        }

        User owner = application.getVacancy().getPostedBy();
        String applicant =
                application.getApplicant() != null &&
                        application.getApplicant().getUsername() != null
                        ? "@" + application.getApplicant().getUsername()
                        : "A weave.ing user";

        saveNotification(
                owner,
                "New vacancy application",
                applicant + " applied for “"
                        + application.getVacancy().getTitle()
                        + "” and submitted a CV.",
                "VACANCY_APPLICATION",
                "/vacancies/" + application.getVacancy().getId()
        );

        try {
            emailService.sendNewVacancyApplicationEmail(application);
        } catch (Exception ignored) {
        }
    }

    @Transactional
    public void notifyVacancyApplicationAccepted(Application application) {

        if (application == null ||
                application.getApplicant() == null ||
                application.getVacancy() == null) {
            return;
        }

        saveNotification(
                application.getApplicant(),
                "Application accepted",
                "Your application for “"
                        + application.getVacancy().getTitle()
                        + "” was accepted by the vacancy owner.",
                "APPLICATION_ACCEPTED",
                "/vacancies/" + application.getVacancy().getId()
        );

        try {
            emailService.sendVacancyApplicationAcceptedEmail(application);
        } catch (Exception ignored) {
        }
    }

    @Transactional
    public void notifyVacancyApplicationRejected(Application application) {

        if (application == null ||
                application.getApplicant() == null ||
                application.getVacancy() == null) {
            return;
        }

        saveNotification(
                application.getApplicant(),
                "Application not accepted",
                "Your application for “"
                        + application.getVacancy().getTitle()
                        + "” was not accepted this time.",
                "APPLICATION_REJECTED",
                "/vacancies/" + application.getVacancy().getId()
        );

        try {
            emailService.sendVacancyApplicationRejectedEmail(application);
        } catch (Exception ignored) {
        }
    }

    public void notifySellerSale(Purchase purchase) {

        if (purchase == null
                || purchase.getPattern() == null
                || purchase.getPattern().getCreator() == null) {
            return;
        }

        User seller = purchase.getPattern().getCreator();

        if (purchase.getBuyer() != null
                && purchase.getBuyer().getId() != null
                && seller.getId() != null
                && seller.getId().equals(purchase.getBuyer().getId())) {
            return;
        }

        saveNotification(
                seller,
                "Pattern sold",
                "Your pattern “" + purchase.getPattern().getTitle()
                        + "” was purchased. Rs "
                        + purchase.getAmount()
                        + " has been added to your earnings.",
                "SALE_CONFIRMED",
                "/dashboard#earnings"
        );

        try {
            emailService.sendSaleNotificationEmail(purchase);
        } catch (Exception ignored) {
        }
    }

    @Transactional
    public void notifyPatternModerationWarning(PatternReport report) {

        if (report == null
                || report.getPattern() == null
                || report.getPattern().getCreator() == null) {
            return;
        }

        User user = report.getPattern().getCreator();

        saveNotification(
                user,
                "Pattern moderation warning",
                "A moderation warning has been issued for your pattern “"
                        + report.getPattern().getTitle()
                        + "”. Please make sure your future patterns follow the Weave.ing community guidelines.",
                "PATTERN_WARNING",
                "/profile#patterns"
        );

        try {
            emailService.sendPatternModerationWarning(report);
        } catch (Exception ignored) {
        }
    }

    private Notification saveNotification(
            User user,
            String title,
            String message,
            String type,
            String targetUrl) {

        Notification notification = new Notification();
        notification.setUser(user);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setType(type);
        notification.setTargetUrl(targetUrl);

        return notificationRepository.save(notification);
    }

    @Transactional
    public String openNotification(Long id, User user) {

        return notificationRepository.findById(id)
                .filter(notification ->
                        notification.getUser().getId().equals(user.getId()))
                .map(notification -> {
                    notification.setRead(true);
                    notificationRepository.save(notification);

                    String targetUrl = notification.getTargetUrl();
                    return targetUrl == null || targetUrl.isBlank()
                            ? "/notifications"
                            : targetUrl;
                })
                .orElse("/notifications");
    }

    @Transactional(readOnly = true)
    public List<Notification> getForUser(User user) {
        return notificationRepository.findByUserOrderByCreatedAtDesc(user);
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(User user) {
        if (user == null) {
            return 0;
        }

        return notificationRepository.countByUserAndReadFalse(user);
    }

    @Transactional
    public void markAsRead(Long id, User user) {

        notificationRepository.findById(id).ifPresent(notification -> {
            if (notification.getUser().getId().equals(user.getId())) {
                notification.setRead(true);
                notificationRepository.save(notification);
            }
        });
    }

    @Transactional
    public void markAllAsRead(User user) {

        List<Notification> notifications =
                notificationRepository.findByUserOrderByCreatedAtDesc(user);

        for (Notification notification : notifications) {
            if (!notification.isRead()) {
                notification.setRead(true);
            }
        }

        notificationRepository.saveAll(notifications);
    }
}