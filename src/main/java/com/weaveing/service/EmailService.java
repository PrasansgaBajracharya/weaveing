package com.weaveing.service;

import com.weaveing.entity.Application;
import com.weaveing.entity.Pattern;
import com.weaveing.entity.Purchase;
import com.weaveing.entity.User;
import com.weaveing.entity.Notification;
import com.weaveing.entity.Vacancy;
import com.weaveing.entity.VacancyReport;
import com.weaveing.entity.PatternReport;
import com.weaveing.entity.Withdrawal;
import com.weaveing.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EmailService {

    private final JavaMailSender mailSender;
    private final UserRepository userRepository;

    @Value("${spring.mail.username}")
    private String senderEmail;

    public EmailService(
            JavaMailSender mailSender,
            UserRepository userRepository) {

        this.mailSender = mailSender;
        this.userRepository = userRepository;
    }

    public void sendVerificationEmail(
            String email,
            String token) {

        String verificationLink =
                "http://localhost:8080/verify?token=" +
                        token;

        SimpleMailMessage message =
                new SimpleMailMessage();

        message.setFrom(senderEmail);
        message.setTo(email);

        message.setSubject(
                "Verify your Weave.ing account"
        );

        message.setText(
                "Welcome to Weave.ing!\n\n" +

                        "Thank you for creating your account.\n\n" +

                        "Please click the link below to verify your email:\n\n" +

                        verificationLink +

                        "\n\n" +

                        "If you did not create this account, " +
                        "you can ignore this email.\n\n" +

                        "— The Weave.ing Team"
        );

        mailSender.send(message);
    }

    public void sendDeviceVerificationEmail(
            String email,
            String token,
            boolean rememberDevice) {

        String verificationLink =
                "http://localhost:8080/verify-device?token=" +
                        token +
                        "&remember=" +
                        rememberDevice;

        SimpleMailMessage message =
                new SimpleMailMessage();

        message.setFrom(senderEmail);
        message.setTo(email);

        message.setSubject(
                "New device login — Weave.ing"
        );

        String choiceText =
                rememberDevice
                        ? "Your selected option is to remember this browser for 30 days."
                        : "Your selected option is to verify this login only.";

        message.setText(
                "A login to your Weave.ing account was made " +
                        "from a new browser or device.\n\n" +

                        choiceText +
                        "\n\n" +

                        "Verify this login here:\n" +
                        verificationLink +
                        "\n\n" +

                        "This verification link expires in 15 minutes.\n\n" +

                        "If you did not make this login attempt, " +
                        "you can safely ignore this email.\n\n" +

                        "— The Weave.ing Team"
        );

        mailSender.send(message);
    }

    public void sendPendingPatternAdminNotification(
            Pattern pattern) {

        if (pattern == null) {
            return;
        }

        List<User> admins =
                userRepository.findByAdminTrue();

        if (admins.isEmpty()) {
            return;
        }

        String creatorUsername =
                pattern.getCreator() != null &&
                        pattern.getCreator().getUsername() != null
                        ? pattern.getCreator().getUsername()
                        : "Unknown creator";

        String category =
                pattern.getCategory() != null
                        ? pattern.getCategory()
                        : "Not specified";

        String difficulty =
                pattern.getDifficulty() != null
                        ? pattern.getDifficulty()
                        : "Not specified";

        String price =
                pattern.isFree()
                        ? "Free"
                        : "Rs " + pattern.getPrice();

        String emailText =
                "Hello Weave.ing Admin,\n\n" +

                        "A new crochet pattern has been submitted " +
                        "and is waiting for review.\n\n" +

                        "Pattern details:\n\n" +

                        "Title: " +
                        pattern.getTitle() +
                        "\n" +

                        "Creator: @" +
                        creatorUsername +
                        "\n" +

                        "Category: " +
                        category +
                        "\n" +

                        "Difficulty: " +
                        difficulty +
                        "\n" +

                        "Price: " +
                        price +
                        "\n\n" +

                        "The pattern is currently marked as PENDING.\n\n" +

                        "Please log in to the Weave.ing admin dashboard " +
                        "to review the PDF and cover image, then approve " +
                        "or reject the submission.\n\n" +

                        "Admin dashboard:\n" +
                        "http://localhost:8080/admin\n\n" +

                        "— The Weave.ing System";

        for (User admin : admins) {

            if (admin.getEmail() == null ||
                    admin.getEmail().isBlank()) {
                continue;
            }

            try {

                SimpleMailMessage message =
                        new SimpleMailMessage();

                message.setFrom(senderEmail);
                message.setTo(admin.getEmail());

                message.setSubject(
                        "New pattern awaiting review — Weave.ing"
                );

                message.setText(emailText);

                mailSender.send(message);

            } catch (Exception ignored) {
            }
        }
    }

    public void sendPatternApprovalEmail(
            Pattern pattern) {

        if (pattern == null ||
                pattern.getCreator() == null ||
                pattern.getCreator().getEmail() == null ||
                pattern.getCreator().getEmail().isBlank()) {

            return;
        }

        String creatorEmail =
                pattern.getCreator().getEmail();

        String creatorName =
                pattern.getCreator().getName();

        if (creatorName == null ||
                creatorName.isBlank()) {

            creatorName =
                    pattern.getCreator().getUsername();
        }

        SimpleMailMessage message =
                new SimpleMailMessage();

        message.setFrom(senderEmail);
        message.setTo(creatorEmail);

        message.setSubject(
                "Your crochet pattern has been approved! — Weave.ing"
        );

        message.setText(
                "Hi " +
                        creatorName +
                        ",\n\n" +

                        "Great news! Your crochet pattern " +
                        "\"" +
                        pattern.getTitle() +
                        "\"" +
                        " has been approved by the Weave.ing admin team.\n\n" +

                        "Your pattern is now available on the " +
                        "Weave.ing marketplace for other users to discover.\n\n" +

                        "Thank you for contributing to the Weave.ing " +
                        "crochet community!\n\n" +

                        "— The Weave.ing Team"
        );

        mailSender.send(message);
    }

    public void sendPatternRejectionEmail(
            Pattern pattern) {

        if (pattern == null ||
                pattern.getCreator() == null ||
                pattern.getCreator().getEmail() == null ||
                pattern.getCreator().getEmail().isBlank()) {

            return;
        }

        String creatorEmail =
                pattern.getCreator().getEmail();

        String creatorName =
                pattern.getCreator().getName();

        if (creatorName == null ||
                creatorName.isBlank()) {

            creatorName =
                    pattern.getCreator().getUsername();
        }

        String rejectionReason =
                pattern.getRejectionReason();

        if (rejectionReason == null ||
                rejectionReason.isBlank()) {

            rejectionReason =
                    "Pattern did not meet the review requirements.";
        }

        SimpleMailMessage message =
                new SimpleMailMessage();

        message.setFrom(senderEmail);
        message.setTo(creatorEmail);

        message.setSubject(
                "Update about your crochet pattern — Weave.ing"
        );

        message.setText(
                "Hi " +
                        creatorName +
                        ",\n\n" +

                        "Thank you for submitting your crochet pattern " +
                        "\"" +
                        pattern.getTitle() +
                        "\"" +
                        " to Weave.ing.\n\n" +

                        "After reviewing your submission, " +
                        "the admin team was unable to approve the pattern " +
                        "for the marketplace at this time.\n\n" +

                        "Reason:\n" +
                        rejectionReason +
                        "\n\n" +

                        "You may review the feedback and make the necessary " +
                        "improvements before submitting the pattern again.\n\n" +

                        "Thank you for being part of the Weave.ing community.\n\n" +

                        "— The Weave.ing Team"
        );

        mailSender.send(message);
    }

    public void sendPurchaseConfirmationEmail(
            Purchase purchase) {

        if (purchase == null ||
                purchase.getBuyer() == null ||
                purchase.getBuyer().getEmail() == null ||
                purchase.getBuyer().getEmail().isBlank() ||
                purchase.getPattern() == null) {

            return;
        }

        String buyerEmail =
                purchase.getBuyer().getEmail();

        String buyerName =
                purchase.getBuyer().getName();

        if (buyerName == null ||
                buyerName.isBlank()) {

            buyerName =
                    purchase.getBuyer().getUsername();
        }

        Pattern pattern =
                purchase.getPattern();

        String creatorUsername =
                pattern.getCreator() != null
                        ? pattern.getCreator().getUsername()
                        : "Unknown creator";

        String transactionId =
                purchase.getTransactionId() != null
                        ? purchase.getTransactionId()
                        : "Not available";

        String purchaseDate =
                purchase.getPurchasedAt() != null
                        ? purchase.getPurchasedAt().toString()
                        : "Not available";

        SimpleMailMessage message =
                new SimpleMailMessage();

        message.setFrom(senderEmail);
        message.setTo(buyerEmail);

        message.setSubject(
                "Purchase confirmed — " +
                        pattern.getTitle() +
                        " — Weave.ing"
        );

        message.setText(
                "Hi " +
                        buyerName +
                        ",\n\n" +

                        "Your purchase on Weave.ing was successful! 🎉\n\n" +

                        "You now own the following digital crochet pattern:\n\n" +

                        "Pattern: " +
                        pattern.getTitle() +
                        "\n" +

                        "Creator: @" +
                        creatorUsername +
                        "\n" +

                        "Amount paid: Rs " +
                        purchase.getAmount() +
                        "\n" +

                        "Transaction ID: " +
                        transactionId +
                        "\n" +

                        "Purchase date: " +
                        purchaseDate +
                        "\n\n" +

                        "Your pattern is now available in your " +
                        "My Library section on Weave.ing.\n\n" +

                        "You can view or download your purchased pattern " +
                        "from your library whenever you need it.\n\n" +

                        "Thank you for supporting crochet creators " +
                        "through Weave.ing!\n\n" +

                        "— The Weave.ing Team"
        );

        mailSender.send(message);
    }

    public void sendNewVacancyApplicationEmail(
            Application application) {

        if (application == null ||
                application.getVacancy() == null ||
                application.getVacancy().getPostedBy() == null) {

            return;
        }

        User vacancyOwner =
                application.getVacancy().getPostedBy();

        if (vacancyOwner.getEmail() == null ||
                vacancyOwner.getEmail().isBlank()) {

            return;
        }

        String ownerName =
                vacancyOwner.getName();

        if (ownerName == null ||
                ownerName.isBlank()) {

            ownerName =
                    vacancyOwner.getUsername();
        }

        String applicantUsername =
                application.getApplicant() != null &&
                        application.getApplicant().getUsername() != null
                        ? application.getApplicant().getUsername()
                        : "A weave.ing user";

        try {
            var mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper =
                    new MimeMessageHelper(mimeMessage, true, "UTF-8");

            helper.setFrom(senderEmail);
            helper.setTo(vacancyOwner.getEmail());
            helper.setSubject(
                    "New application for your weave.ing vacancy"
            );

            helper.setText(
                    "Hi " +
                            ownerName +
                            ",\n\n" +

                            "@" +
                            applicantUsername +
                            " has applied for your vacancy:\n\n" +

                            application.getVacancy().getTitle() +
                            "\n\n" +

                            "Application message:\n" +
                            (
                                    application.getMessage() != null
                                            ? application.getMessage()
                                            : "No message provided."
                            ) +
                            "\n\n" +

                            "A CV was submitted with this application. " +
                            "The CV is also available securely from the application page.\n\n" +

                            "Log in to Weave.ing to review the application.\n\n" +

                            "— The Weave.ing Team"
            );

            if (application.getCvData() != null &&
                    application.getCvData().length > 0) {

                String fileName =
                        application.getCvFileName() != null &&
                                !application.getCvFileName().isBlank()
                                ? application.getCvFileName()
                                : "cv.pdf";

                String contentType =
                        application.getCvContentType() != null &&
                                !application.getCvContentType().isBlank()
                                ? application.getCvContentType()
                                : "application/pdf";

                helper.addAttachment(
                        fileName,
                        new ByteArrayResource(application.getCvData()),
                        contentType
                );
            }

            mailSender.send(mimeMessage);

        } catch (Exception e) {
            throw new IllegalStateException(
                    "Unable to send vacancy application email",
                    e
            );
        }
    }

    public void sendVacancyApplicationAcceptedEmail(
            Application application) {

        if (application == null ||
                application.getApplicant() == null ||
                application.getApplicant().getEmail() == null ||
                application.getApplicant().getEmail().isBlank() ||
                application.getVacancy() == null) {

            return;
        }

        String applicantEmail =
                application.getApplicant().getEmail();

        String applicantName =
                application.getApplicant().getName();

        if (applicantName == null ||
                applicantName.isBlank()) {

            applicantName =
                    application.getApplicant().getUsername();
        }

        SimpleMailMessage message =
                new SimpleMailMessage();

        message.setFrom(senderEmail);
        message.setTo(applicantEmail);

        message.setSubject(
                "Your weave.ing vacancy application was accepted"
        );

        message.setText(
                "Hi " +
                        applicantName +
                        ",\n\n" +

                        "Good news! Your application for the vacancy:\n\n" +

                        application.getVacancy().getTitle() +
                        "\n\n" +

                        "has been accepted by the vacancy owner.\n\n" +

                        "Log in to Weave.ing to view the vacancy and your application.\n\n" +

                        "— The Weave.ing Team"
        );

        mailSender.send(message);
    }

    public void sendVacancyApplicationRejectedEmail(
            Application application) {

        if (application == null ||
                application.getApplicant() == null ||
                application.getApplicant().getEmail() == null ||
                application.getApplicant().getEmail().isBlank() ||
                application.getVacancy() == null) {

            return;
        }

        String applicantEmail =
                application.getApplicant().getEmail();

        String applicantName =
                application.getApplicant().getName();

        if (applicantName == null ||
                applicantName.isBlank()) {

            applicantName =
                    application.getApplicant().getUsername();
        }

        SimpleMailMessage message =
                new SimpleMailMessage();

        message.setFrom(senderEmail);
        message.setTo(applicantEmail);

        message.setSubject(
                "Update on your weave.ing vacancy application"
        );

        message.setText(
                "Hi " +
                        applicantName +
                        ",\n\n" +

                        "The vacancy owner has reviewed your application for:\n\n" +

                        application.getVacancy().getTitle() +
                        "\n\n" +

                        "Unfortunately, your application was not accepted this time.\n\n" +

                        "Thank you for using Weave.ing and for taking the time to apply.\n\n" +

                        "— The Weave.ing Team"
        );

        mailSender.send(message);
    }

    public void sendVacancyReportAdminNotification(
            VacancyReport report) {

        if (report == null ||
                report.getVacancy() == null) {

            return;
        }

        List<User> admins =
                userRepository.findByAdminTrue();

        if (admins.isEmpty()) {
            return;
        }

        String vacancyTitle =
                report.getVacancy().getTitle();

        String reporterUsername =
                report.getReportedBy() != null &&
                        report.getReportedBy().getUsername() != null
                        ? report.getReportedBy().getUsername()
                        : "Unknown user";

        String ownerUsername =
                report.getVacancy().getPostedBy() != null &&
                        report.getVacancy().getPostedBy().getUsername() != null
                        ? report.getVacancy().getPostedBy().getUsername()
                        : "Unknown user";

        String details =
                report.getDetails() != null &&
                        !report.getDetails().isBlank()
                        ? report.getDetails()
                        : "No additional details were provided.";

        String emailText =
                "Hello Weave.ing Admin,\n\n" +

                        "A vacancy has been reported and is waiting for moderation review.\n\n" +

                        "Vacancy: " +
                        vacancyTitle +
                        "\n" +

                        "Posted by: @" +
                        ownerUsername +
                        "\n" +

                        "Reported by: @" +
                        reporterUsername +
                        "\n" +

                        "Reason: " +
                        report.getReason() +
                        "\n\n" +

                        "Additional details:\n" +
                        details +
                        "\n\n" +

                        "Please review the report from the admin dashboard:\n" +
                        "http://localhost:8080/admin/vacancies\n\n" +

                        "— The Weave.ing System";

        for (User admin : admins) {

            if (admin.getEmail() == null ||
                    admin.getEmail().isBlank()) {

                continue;
            }

            try {

                SimpleMailMessage message =
                        new SimpleMailMessage();

                message.setFrom(senderEmail);
                message.setTo(admin.getEmail());

                message.setSubject(
                        "New vacancy report — Weave.ing"
                );

                message.setText(emailText);

                mailSender.send(message);

            } catch (Exception ignored) {
            }
        }
    }

    public void sendVacancyReportUserNotification(
            VacancyReport report) {

        if (report == null ||
                report.getVacancy() == null ||
                report.getVacancy().getPostedBy() == null ||
                report.getVacancy().getPostedBy().getEmail() == null ||
                report.getVacancy().getPostedBy().getEmail().isBlank()) {

            return;
        }

        User reportedUser =
                report.getVacancy().getPostedBy();

        String name =
                reportedUser.getName();

        if (name == null ||
                name.isBlank()) {

            name =
                    reportedUser.getUsername();
        }

        SimpleMailMessage message =
                new SimpleMailMessage();

        message.setFrom(senderEmail);
        message.setTo(reportedUser.getEmail());

        message.setSubject(
                "A vacancy associated with your account was reported — Weave.ing"
        );

        message.setText(
                "Hi " +
                        name +
                        ",\n\n" +

                        "A report has been submitted regarding the vacancy " +
                        "\"" +
                        report.getVacancy().getTitle() +
                        "\" on Weave.ing.\n\n" +

                        "Our moderation team will review the report and the " +
                        "content involved. A report does not automatically mean " +
                        "that a violation has been confirmed.\n\n" +

                        "Please make sure your vacancies follow the Weave.ing " +
                        "community guidelines. Repeated or confirmed violations " +
                        "may result in content removal, account restrictions, " +
                        "suspension, or account removal.\n\n" +

                        "No action is required from you at this stage unless the " +
                        "moderation team contacts you with a specific request.\n\n" +

                        "— The Weave.ing Team"
        );

        mailSender.send(message);
    }

    public void sendVacancyModerationWarning(
            VacancyReport report) {

        if (report == null ||
                report.getVacancy() == null ||
                report.getVacancy().getPostedBy() == null) {
            return;
        }

        User user = report.getVacancy().getPostedBy();

        if (user.getEmail() == null ||
                user.getEmail().isBlank()) {
            return;
        }

        String name = user.getName();

        if (name == null || name.isBlank()) {
            name = user.getUsername();
        }

        SimpleMailMessage message =
                new SimpleMailMessage();

        message.setFrom(senderEmail);
        message.setTo(user.getEmail());
        message.setSubject(
                "Moderation warning regarding your weave.ing vacancy"
        );

        message.setText(
                "Hi " + name + ",\n\n" +
                        "Our moderation team has reviewed a report " +
                        "submitted about your vacancy:\n\n" +
                        report.getVacancy().getTitle() + "\n\n" +
                        "The report was reviewed and a moderation warning " +
                        "has been issued for this content.\n\n" +
                        "Please make sure your future vacancies follow the " +
                        "Weave.ing community guidelines. Continued or serious " +
                        "violations may result in content removal, account " +
                        "restrictions, suspension, or account removal.\n\n" +
                        "Thank you for helping us keep Weave.ing useful and welcoming.\n\n" +
                        "— The Weave.ing Moderation Team"
        );

        mailSender.send(message);
    }

    public void sendVacancyRemovedNotification(
            Vacancy vacancy) {

        if (vacancy == null ||
                vacancy.getPostedBy() == null ||
                vacancy.getPostedBy().getEmail() == null ||
                vacancy.getPostedBy().getEmail().isBlank()) {
            return;
        }

        User user = vacancy.getPostedBy();

        String name = user.getName();

        if (name == null || name.isBlank()) {
            name = user.getUsername();
        }

        SimpleMailMessage message =
                new SimpleMailMessage();

        message.setFrom(senderEmail);
        message.setTo(user.getEmail());
        message.setSubject(
                "Your weave.ing vacancy was removed"
        );

        message.setText(
                "Hi " + name + ",\n\n" +
                        "Your vacancy:\n\n" +
                        vacancy.getTitle() + "\n\n" +
                        "has been removed from the Weave.ing vacancy board " +
                        "following moderation review.\n\n" +
                        "This action was taken to keep the community safe " +
                        "and the vacancy board useful for users.\n\n" +
                        "Please review the Weave.ing community guidelines before " +
                        "posting another vacancy. Repeated or serious violations " +
                        "may result in further account restrictions, suspension, " +
                        "or account removal.\n\n" +
                        "— The Weave.ing Moderation Team"
        );

        mailSender.send(message);
    }

    public void sendPatternReportAdminNotification(PatternReport report) {
        if (report == null || report.getPattern() == null) return;
        List<User> admins = userRepository.findByAdminTrue();
        String title = report.getPattern().getTitle();
        String creator = report.getPattern().getCreator() == null ? "Unknown user" : report.getPattern().getCreator().getUsername();
        String reporter = report.getReportedBy() == null ? "Unknown user" : report.getReportedBy().getUsername();
        String details = report.getDetails() == null || report.getDetails().isBlank() ? "No additional details were provided." : report.getDetails();
        String text = "Hello Weave.ing Admin,\n\n" +
                "A pattern has been reported and is waiting for moderation review.\n\n" +
                "Pattern: " + title + "\n" +
                "Created by: @" + creator + "\n" +
                "Reported by: @" + reporter + "\n" +
                "Reason: " + report.getReason() + "\n\n" +
                "Additional details:\n" + details + "\n\n" +
                "Please review the report from the admin dashboard:\n" +
                "http://localhost:8080/admin/pattern-reports\n\n" +
                "— The Weave.ing System";
        for (User admin : admins) {
            if (admin.getEmail() == null || admin.getEmail().isBlank()) continue;
            try {
                SimpleMailMessage message = new SimpleMailMessage();
                message.setFrom(senderEmail);
                message.setTo(admin.getEmail());
                message.setSubject("New pattern report — Weave.ing");
                message.setText(text);
                mailSender.send(message);
            } catch (Exception ignored) {
            }
        }
    }

    public void sendPatternReportUserNotification(PatternReport report) {
        if (report == null || report.getPattern() == null || report.getPattern().getCreator() == null) return;
        User user = report.getPattern().getCreator();
        if (user.getEmail() == null || user.getEmail().isBlank()) return;
        String name = user.getName() == null || user.getName().isBlank() ? user.getUsername() : user.getName();
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(senderEmail);
        message.setTo(user.getEmail());
        message.setSubject("A pattern associated with your account was reported — Weave.ing");
        message.setText("Hi " + name + ",\n\n" +
                "A report has been submitted regarding your pattern \"" + report.getPattern().getTitle() + "\" on Weave.ing.\n\n" +
                "Our moderation team will review the report and the content involved. A report does not automatically mean that a violation has been confirmed.\n\n" +
                "Please make sure your patterns follow the Weave.ing community guidelines. Repeated or confirmed violations may result in content removal or account restrictions.\n\n" +
                "— The Weave.ing Team");
        mailSender.send(message);
    }

    public void sendPatternModerationWarning(PatternReport report) {
        if (report == null || report.getPattern() == null || report.getPattern().getCreator() == null) return;
        User user = report.getPattern().getCreator();
        if (user.getEmail() == null || user.getEmail().isBlank()) return;
        String name = user.getName() == null || user.getName().isBlank() ? user.getUsername() : user.getName();
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(senderEmail);
        message.setTo(user.getEmail());
        message.setSubject("Moderation warning regarding your weave.ing pattern");
        message.setText("Hi " + name + ",\n\n" +
                "Our moderation team has reviewed a report submitted about your pattern:\n\n" +
                report.getPattern().getTitle() + "\n\n" +
                "A moderation warning has been issued for this content. Please make sure future patterns follow the Weave.ing community guidelines. Continued or serious violations may result in content removal or account restrictions.\n\n" +
                "— The Weave.ing Moderation Team");
        mailSender.send(message);
    }

    public void sendPatternAccountBanNotification(PatternReport report) {
        if (report == null || report.getPattern() == null || report.getPattern().getCreator() == null) return;
        User user = report.getPattern().getCreator();
        if (user.getEmail() == null || user.getEmail().isBlank()) return;
        String name = user.getName() == null || user.getName().isBlank() ? user.getUsername() : user.getName();
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(senderEmail);
        message.setTo(user.getEmail());
        message.setSubject("Your Weave.ing account has been restricted");
        message.setText("Hi " + name + ",\n\n" +
                "Your Weave.ing account has been banned following moderation review of a report concerning your pattern:\n\n" +
                report.getPattern().getTitle() + "\n\n" +
                "You will no longer be able to sign in to the account.\n\n" +
                "— The Weave.ing Moderation Team");
        mailSender.send(message);
    }

    public void sendPatternRemovedNotification(Pattern pattern) {
        if (pattern == null || pattern.getCreator() == null || pattern.getCreator().getEmail() == null || pattern.getCreator().getEmail().isBlank()) return;
        User user = pattern.getCreator();
        String name = user.getName() == null || user.getName().isBlank() ? user.getUsername() : user.getName();
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(senderEmail);
        message.setTo(user.getEmail());
        message.setSubject("Your weave.ing pattern was removed");
        message.setText("Hi " + name + ",\n\nYour pattern:\n\n" + pattern.getTitle() + "\n\nhas been removed from the Weave.ing marketplace following moderation review.\n\nPlease review the Weave.ing community guidelines before submitting another pattern.\n\n— The Weave.ing Moderation Team");
        mailSender.send(message);
    }

    public void sendWithdrawalPendingUserNotification(
            Withdrawal withdrawal) {

        if (withdrawal == null ||
                withdrawal.getUser() == null ||
                withdrawal.getUser().getEmail() == null ||
                withdrawal.getUser().getEmail().isBlank()) {
            return;
        }

        User user = withdrawal.getUser();
        String name = user.getName();

        if (name == null || name.isBlank()) {
            name = user.getUsername();
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(senderEmail);
        message.setTo(user.getEmail());
        message.setSubject("Withdrawal request received — Weave.ing");
        message.setText(
                "Hi " + name + ",\n\n" +
                "Your withdrawal request has been received by Weave.ing.\n\n" +
                "Amount: Rs " + withdrawal.getAmount() + "\n" +
                "Method: " + withdrawal.getPaymentMethod() + "\n" +
                "Account: " + withdrawal.getAccountIdentifier() + "\n" +
                "Status: PENDING\n\n" +
                "An administrator will review your request. You will receive another email when the request is approved or rejected.\n\n" +
                "— The Weave.ing Team"
        );

        mailSender.send(message);
    }

    public void sendWithdrawalPendingAdminNotification(
            Withdrawal withdrawal) {

        if (withdrawal == null) {
            return;
        }

        List<User> admins = userRepository.findByAdminTrue();

        if (admins.isEmpty()) {
            return;
        }

        User user = withdrawal.getUser();
        String username = user != null ? user.getUsername() : "Unknown user";
        String email = user != null ? user.getEmail() : "Unknown email";

        String emailText =
                "Hello Weave.ing Admin,\n\n" +
                "A new withdrawal request is waiting for approval.\n\n" +
                "Seller: @" + username + "\n" +
                "Email: " + email + "\n" +
                "Amount: Rs " + withdrawal.getAmount() + "\n" +
                "Method: " + withdrawal.getPaymentMethod() + "\n" +
                "Account: " + withdrawal.getAccountIdentifier() + "\n" +
                "Status: PENDING\n\n" +
                "Please log in to the admin dashboard and review the request.\n\n" +
                "Admin payments dashboard:\n" +
                "http://localhost:8080/admin/payments\n\n" +
                "— The Weave.ing System";

        for (User admin : admins) {
            if (admin.getEmail() == null || admin.getEmail().isBlank()) {
                continue;
            }

            try {
                SimpleMailMessage message = new SimpleMailMessage();
                message.setFrom(senderEmail);
                message.setTo(admin.getEmail());
                message.setSubject("New withdrawal request awaiting approval — Weave.ing");
                message.setText(emailText);
                mailSender.send(message);
            } catch (Exception ignored) {
            }
        }
    }

    public void sendWithdrawalApprovalEmail(
            Withdrawal withdrawal) {

        sendWithdrawalStatusEmail(
                withdrawal,
                "Withdrawal approved — Weave.ing",
                "Your withdrawal request has been approved by the Weave.ing admin team.",
                "COMPLETED"
        );
    }

    public void sendWithdrawalRejectionEmail(
            Withdrawal withdrawal) {

        sendWithdrawalStatusEmail(
                withdrawal,
                "Withdrawal request rejected — Weave.ing",
                "Your withdrawal request has been rejected by the Weave.ing admin team. The amount has been returned to your available balance.",
                "REJECTED"
        );
    }

    private void sendWithdrawalStatusEmail(
            Withdrawal withdrawal,
            String subject,
            String statusMessage,
            String status) {

        if (withdrawal == null ||
                withdrawal.getUser() == null ||
                withdrawal.getUser().getEmail() == null ||
                withdrawal.getUser().getEmail().isBlank()) {
            return;
        }

        User user = withdrawal.getUser();
        String name = user.getName();

        if (name == null || name.isBlank()) {
            name = user.getUsername();
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(senderEmail);
        message.setTo(user.getEmail());
        message.setSubject(subject);
        message.setText(
                "Hi " + name + ",\n\n" +
                statusMessage + "\n\n" +
                "Amount: Rs " + withdrawal.getAmount() + "\n" +
                "Method: " + withdrawal.getPaymentMethod() + "\n" +
                "Account: " + withdrawal.getAccountIdentifier() + "\n" +
                "Status: " + status + "\n\n" +
                "— The Weave.ing Team"
        );

        mailSender.send(message);
    }

    public void sendSaleNotificationEmail(
            Purchase purchase) {

        if (purchase == null ||
                purchase.getPattern() == null ||
                purchase.getPattern().getCreator() == null ||
                purchase.getPattern().getCreator().getEmail() == null ||
                purchase.getPattern().getCreator().getEmail().isBlank()) {
            return;
        }

        User seller = purchase.getPattern().getCreator();
        String sellerName = seller.getName();

        if (sellerName == null || sellerName.isBlank()) {
            sellerName = seller.getUsername();
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(senderEmail);
        message.setTo(seller.getEmail());
        message.setSubject(
                "Your pattern was purchased — Weave.ing"
        );
        message.setText(
                "Hi " + sellerName + ",\n\n" +
                        "Your crochet pattern \"" +
                        purchase.getPattern().getTitle() +
                        "\" has been purchased successfully.\n\n" +
                        "Amount added to your Weave.ing earnings: Rs " +
                        purchase.getAmount() +
                        "\n\n" +
                        "You can view your updated earnings in your dashboard.\n\n" +
                        "— The Weave.ing Team"
        );

        try {
            mailSender.send(message);
        } catch (Exception ignored) {
        }
    }

    public void sendNotificationEmail(
            Notification notification) {

        if (notification == null ||
                notification.getUser() == null ||
                notification.getUser().getEmail() == null ||
                notification.getUser().getEmail().isBlank()) {
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(senderEmail);
            message.setTo(notification.getUser().getEmail());
            message.setSubject(notification.getTitle() + " — Weave.ing");
            message.setText(
                    notification.getMessage() +
                            "\n\n" +
                            "You can view this notification in your Weave.ing account."
            );
            mailSender.send(message);
        } catch (Exception ignored) {
        }
    }

}