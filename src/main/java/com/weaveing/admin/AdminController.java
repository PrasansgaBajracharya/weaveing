package com.weaveing.admin;

import com.weaveing.entity.Pattern;
import com.weaveing.entity.User;
import com.weaveing.entity.Vacancy;
import com.weaveing.entity.VacancyReport;
import com.weaveing.entity.PatternReport;
import com.weaveing.entity.Purchase;
import com.weaveing.entity.Withdrawal;
import com.weaveing.repository.PatternRepository;
import com.weaveing.repository.UserRepository;
import com.weaveing.repository.VacancyReportRepository;
import com.weaveing.repository.VacancyRepository;
import com.weaveing.repository.PatternReportRepository;
import com.weaveing.repository.PurchaseRepository;
import com.weaveing.repository.WithdrawalRepository;
import com.weaveing.service.EmailService;
import com.weaveing.service.NotificationService;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final PatternRepository patternRepository;
    private final UserRepository userRepository;
    private final VacancyRepository vacancyRepository;
    private final VacancyReportRepository vacancyReportRepository;
    private final PatternReportRepository patternReportRepository;
    private final PurchaseRepository purchaseRepository;
    private final WithdrawalRepository withdrawalRepository;
    private final EmailService emailService;
    private final NotificationService notificationService;

    public AdminController(
            PatternRepository patternRepository,
            UserRepository userRepository,
            VacancyRepository vacancyRepository,
            VacancyReportRepository vacancyReportRepository,
            PatternReportRepository patternReportRepository,
            PurchaseRepository purchaseRepository,
            WithdrawalRepository withdrawalRepository,
            EmailService emailService,
            NotificationService notificationService) {

        this.patternRepository = patternRepository;
        this.userRepository = userRepository;
        this.vacancyRepository = vacancyRepository;
        this.vacancyReportRepository = vacancyReportRepository;
        this.patternReportRepository = patternReportRepository;
        this.purchaseRepository = purchaseRepository;
        this.withdrawalRepository = withdrawalRepository;
        this.emailService = emailService;
        this.notificationService = notificationService;
    }

    @GetMapping
    public String adminDashboard(Model model) {

        model.addAttribute(
                "pendingCount",
                patternRepository.findByApprovalStatus(
                        Pattern.ApprovalStatus.PENDING
                ).size()
        );

        model.addAttribute(
                "approvedCount",
                patternRepository.findByApprovalStatus(
                        Pattern.ApprovalStatus.APPROVED
                ).size()
        );

        model.addAttribute(
                "rejectedCount",
                patternRepository.findByApprovalStatus(
                        Pattern.ApprovalStatus.REJECTED
                ).size()
        );

        model.addAttribute(
                "userCount",
                userRepository.count()
        );

        long openVacancyCount =
                vacancyRepository.findAllByOrderByCreatedAtDesc()
                        .stream()
                        .filter(v -> !v.isRemoved())
                        .filter(v -> v.getStatus() == Vacancy.Status.OPEN)
                        .count();

        model.addAttribute(
                "openVacancyCount",
                openVacancyCount
        );

        addAdminNavigation(model);

        return "admin-dashboard";
    }

    @GetMapping("/patterns")
    public String patternManagement(Model model) {

        List<Pattern> pendingPatterns =
                patternRepository.findByApprovalStatus(
                        Pattern.ApprovalStatus.PENDING
                );

        List<Pattern> approvedPatterns =
                patternRepository.findByApprovalStatus(
                        Pattern.ApprovalStatus.APPROVED
                );

        List<Pattern> rejectedPatterns =
                patternRepository.findByApprovalStatus(
                        Pattern.ApprovalStatus.REJECTED
                );

        model.addAttribute("pendingPatterns", pendingPatterns);
        model.addAttribute("approvedPatterns", approvedPatterns);
        model.addAttribute("rejectedPatterns", rejectedPatterns);

        addAdminNavigation(model);

        return "admin-patterns";
    }


    @GetMapping("/pattern-reports")
    public String patternReports(Model model) {

        List<PatternReport> patternReports =
                patternReportRepository.findAllByOrderByReportedAtDesc();

        model.addAttribute("patternReports", patternReports);
        model.addAttribute(
                "pendingPatternReportCount",
                patternReportRepository.countByStatus(PatternReport.Status.PENDING)
        );

        addAdminNavigation(model);

        return "admin-pattern-reports";
    }

    @GetMapping("/notifications")
    public String notifications(Model model) {

        List<Pattern> pendingPatterns =
                patternRepository.findByApprovalStatus(
                        Pattern.ApprovalStatus.PENDING
                );

        List<VacancyReport> pendingReports =
                vacancyReportRepository.findByStatusOrderByReportedAtDesc(
                        VacancyReport.Status.PENDING
                );

        List<PatternReport> pendingPatternReports =
                patternReportRepository.findByStatusOrderByReportedAtDesc(
                        PatternReport.Status.PENDING
                );

        model.addAttribute("pendingPatterns", pendingPatterns);
        model.addAttribute("pendingReports", pendingReports);
        model.addAttribute("pendingPatternReports", pendingPatternReports);

        addAdminNavigation(model);

        return "admin-notifications";
    }

    @GetMapping("/patterns/{id}/review")
    public ResponseEntity<Resource> reviewPattern(
            @PathVariable Long id) {

        Pattern pattern =
                patternRepository.findById(id)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Pattern not found"
                                )
                        );

        String filePath = pattern.getFilePath();

        if (filePath == null || filePath.isBlank()) {
            return ResponseEntity.notFound().build();
        }

        String filename = filePath;

        if (filename.startsWith("/uploads/private-patterns/")) {
            filename = filename.substring(
                    "/uploads/private-patterns/".length()
            );
        } else if (filename.startsWith("/uploads/patterns/")) {
            filename = filename.substring(
                    "/uploads/patterns/".length()
            );
        } else {
            return ResponseEntity.notFound().build();
        }

        Path privateDirectory =
                Paths.get("uploads", "private-patterns")
                        .toAbsolutePath()
                        .normalize();

        Path patternDirectory =
                Paths.get("uploads", "patterns")
                        .toAbsolutePath()
                        .normalize();

        Path resolvedPath;

        if (filePath.startsWith("/uploads/private-patterns/")) {
            resolvedPath =
                    privateDirectory.resolve(filename).normalize();

            if (!resolvedPath.startsWith(privateDirectory)) {
                return ResponseEntity.status(403).build();
            }
        } else {
            resolvedPath =
                    patternDirectory.resolve(filename).normalize();

            if (!resolvedPath.startsWith(patternDirectory)) {
                return ResponseEntity.status(403).build();
            }
        }

        if (!Files.exists(resolvedPath) ||
                !Files.isRegularFile(resolvedPath)) {
            return ResponseEntity.notFound().build();
        }

        Resource resource =
                new FileSystemResource(resolvedPath);

        String safeFilename =
                pattern.getTitle() == null
                        ? "pattern.pdf"
                        : pattern.getTitle()
                        .replaceAll("[^a-zA-Z0-9-_ ]", "")
                        .trim()
                        .replaceAll("\\s+", "-")
                        + ".pdf";

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"" + safeFilename + "\""
                )
                .body(resource);
    }

    @GetMapping("/users")
    public String userManagement(Model model) {

        model.addAttribute(
                "users",
                userRepository.findAll()
        );

        addAdminNavigation(model);

        return "admin-users";
    }

    @GetMapping("/vacancies")
    public String vacancyManagement(Model model) {

        List<Vacancy> vacancies =
                vacancyRepository.findAllByOrderByCreatedAtDesc();

        List<VacancyReport> reports =
                vacancyReportRepository.findAllByOrderByReportedAtDesc();

        long openCount = vacancies.stream()
                .filter(v -> !v.isRemoved())
                .filter(v -> v.getStatus() == Vacancy.Status.OPEN)
                .count();

        long closedCount = vacancies.stream()
                .filter(v -> !v.isRemoved())
                .filter(v -> v.getStatus() == Vacancy.Status.CLOSED)
                .count();

        long removedCount = vacancies.stream()
                .filter(Vacancy::isRemoved)
                .count();

        long pendingReportCount = reports.stream()
                .filter(r -> r.getStatus() == VacancyReport.Status.PENDING)
                .count();

        Map<Long, Long> reportCounts = new HashMap<>();

        for (VacancyReport report : reports) {
            if (report.getVacancy() != null &&
                    report.getVacancy().getId() != null) {

                Long vacancyId = report.getVacancy().getId();

                reportCounts.put(
                        vacancyId,
                        reportCounts.getOrDefault(vacancyId, 0L) + 1
                );
            }
        }

        model.addAttribute("vacancies", vacancies);
        model.addAttribute("reports", reports);
        model.addAttribute("reportCounts", reportCounts);
        model.addAttribute("vacancyCount", vacancies.size());
        model.addAttribute("openCount", openCount);
        model.addAttribute("closedCount", closedCount);
        model.addAttribute("removedCount", removedCount);
        model.addAttribute("pendingReportCount", pendingReportCount);

        addAdminNavigation(model);

        return "admin-vacancies";
    }

    @GetMapping("/payments")
    public String paymentManagement(Model model) {

        List<Purchase> purchases =
                purchaseRepository.findAll();

        List<Purchase> verifiedSales =
                purchases.stream()
                        .filter(purchase ->
                                purchase.getPaymentStatus() ==
                                        Purchase.PaymentStatus.VERIFIED)
                        .sorted((a, b) -> b.getPurchasedAt().compareTo(a.getPurchasedAt()))
                        .toList();

        List<Withdrawal> withdrawals =
                withdrawalRepository.findAll();

        withdrawals.sort((a, b) ->
                b.getRequestedAt().compareTo(a.getRequestedAt()));

        double totalSales =
                verifiedSales.stream()
                        .mapToDouble(Purchase::getAmount)
                        .sum();

        double totalWithdrawn =
                withdrawals.stream()
                        .filter(withdrawal ->
                                withdrawal.getStatus() == Withdrawal.Status.COMPLETED)
                        .mapToDouble(Withdrawal::getAmount)
                        .sum();

        double pendingWithdrawals =
                withdrawals.stream()
                        .filter(withdrawal ->
                                withdrawal.getStatus() == Withdrawal.Status.PENDING)
                        .mapToDouble(Withdrawal::getAmount)
                        .sum();

        long pendingWithdrawalCount =
                withdrawals.stream()
                        .filter(withdrawal ->
                                withdrawal.getStatus() == Withdrawal.Status.PENDING)
                        .count();

        model.addAttribute("sales", verifiedSales);
        model.addAttribute("withdrawals", withdrawals);
        model.addAttribute("totalSales", totalSales);
        model.addAttribute("totalWithdrawn", totalWithdrawn);
        model.addAttribute("pendingWithdrawals", pendingWithdrawals);
        model.addAttribute("pendingWithdrawalCount", pendingWithdrawalCount);

        addAdminNavigation(model);

        return "admin-payments";
    }

    @PostMapping("/withdrawals/{id}/approve")
    public String approveWithdrawal(@PathVariable Long id) {

        Withdrawal withdrawal = findWithdrawal(id);

        if (withdrawal.getStatus() == Withdrawal.Status.PENDING) {
            withdrawal.setStatus(Withdrawal.Status.COMPLETED);
            withdrawalRepository.save(withdrawal);

            try {
                emailService.sendWithdrawalApprovalEmail(withdrawal);
            } catch (Exception ignored) {
            }
        }

        return "redirect:/admin/payments#withdrawals";
    }

    @PostMapping("/withdrawals/{id}/reject")
    public String rejectWithdrawal(@PathVariable Long id) {

        Withdrawal withdrawal = findWithdrawal(id);

        if (withdrawal.getStatus() == Withdrawal.Status.PENDING) {
            withdrawal.setStatus(Withdrawal.Status.REJECTED);
            withdrawalRepository.save(withdrawal);

            try {
                emailService.sendWithdrawalRejectionEmail(withdrawal);
            } catch (Exception ignored) {
            }
        }

        return "redirect:/admin/payments#withdrawals";
    }

    @PostMapping("/patterns/{id}/approve")
    public String approvePattern(@PathVariable Long id) {

        Pattern pattern =
                patternRepository.findById(id)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Pattern not found"
                                )
                        );

        pattern.setApprovalStatus(
                Pattern.ApprovalStatus.APPROVED
        );

        pattern.setRejectionReason(null);
        pattern.setReviewedAt(LocalDateTime.now());
        patternRepository.save(pattern);

        notificationService.notifyPatternApproved(pattern);

        return "redirect:/admin/patterns";
    }

    @PostMapping("/patterns/{id}/reject")
    public String rejectPattern(
            @PathVariable Long id,
            @RequestParam(
                    value = "rejectionReason",
                    defaultValue = ""
            ) String rejectionReason) {

        Pattern pattern =
                patternRepository.findById(id)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Pattern not found"
                                )
                        );

        pattern.setApprovalStatus(
                Pattern.ApprovalStatus.REJECTED
        );

        String reason = rejectionReason.trim();

        if (reason.isEmpty()) {
            reason =
                    "Pattern did not meet the review requirements.";
        }

        pattern.setRejectionReason(reason);
        pattern.setReviewedAt(LocalDateTime.now());
        patternRepository.save(pattern);

        notificationService.notifyPatternRejected(pattern);

        return "redirect:/admin/patterns";
    }

    @PostMapping("/pattern-reports/{reportId}/dismiss")
    public String dismissPatternReport(@PathVariable Long reportId) {
        PatternReport report = findPatternReport(reportId);
        if (report.getStatus() == PatternReport.Status.PENDING) {
            report.setStatus(PatternReport.Status.DISMISSED);
            report.setReviewedAt(LocalDateTime.now());
            patternReportRepository.save(report);
        }
        return "redirect:/admin/patterns";
    }

    @PostMapping("/pattern-reports/{reportId}/warn")
    public String warnPatternUser(@PathVariable Long reportId) {
        PatternReport report = findPatternReport(reportId);
        if (report.getStatus() == PatternReport.Status.PENDING) {
            report.setStatus(PatternReport.Status.ACTION_TAKEN);
            report.setReviewedAt(LocalDateTime.now());
            patternReportRepository.save(report);
            notificationService.notifyPatternModerationWarning(report);
        }
        return "redirect:/admin/patterns";
    }

    @PostMapping("/pattern-reports/{reportId}/ban")
    public String banPatternUser(@PathVariable Long reportId) {
        PatternReport report = findPatternReport(reportId);
        if (report.getStatus() == PatternReport.Status.PENDING) {
            User creator = report.getPattern().getCreator();
            if (creator != null && !creator.isAdmin()) {
                creator.setBanned(true);
                userRepository.save(creator);
                report.setStatus(PatternReport.Status.ACTION_TAKEN);
                report.setReviewedAt(LocalDateTime.now());
                patternReportRepository.save(report);
                try {
                    emailService.sendPatternAccountBanNotification(report);
                } catch (Exception ignored) {
                }
            }
        }
        return "redirect:/admin/patterns";
    }

    @PostMapping("/pattern-reports/{reportId}/remove")
    public String removeReportedPattern(@PathVariable Long reportId) {
        PatternReport report = findPatternReport(reportId);
        Pattern pattern = report.getPattern();
        if (pattern.getRemovedAt() == null) {
            pattern.setRemovedAt(LocalDateTime.now());
            patternRepository.save(pattern);
        }
        if (report.getStatus() == PatternReport.Status.PENDING) {
            report.setStatus(PatternReport.Status.ACTION_TAKEN);
            report.setReviewedAt(LocalDateTime.now());
            patternReportRepository.save(report);
        }
        try {
            emailService.sendPatternRemovedNotification(pattern);
        } catch (Exception ignored) {
        }
        return "redirect:/admin/patterns";
    }

    @PostMapping("/vacancies/{id}/close")
    public String closeVacancy(@PathVariable Long id) {

        Vacancy vacancy = findVacancy(id);

        if (!vacancy.isRemoved()) {
            vacancy.setStatus(Vacancy.Status.CLOSED);
            vacancyRepository.save(vacancy);
        }

        return "redirect:/admin/vacancies";
    }

    @PostMapping("/vacancies/{id}/open")
    public String openVacancy(@PathVariable Long id) {

        Vacancy vacancy = findVacancy(id);

        if (!vacancy.isRemoved()) {
            vacancy.setStatus(Vacancy.Status.OPEN);
            vacancyRepository.save(vacancy);
        }

        return "redirect:/admin/vacancies";
    }

    @PostMapping("/vacancies/{id}/remove")
    public String removeVacancy(@PathVariable Long id) {

        Vacancy vacancy = findVacancy(id);

        if (!vacancy.isRemoved()) {
            vacancy.setRemoved(true);
            vacancy.setStatus(Vacancy.Status.CLOSED);
            vacancyRepository.save(vacancy);

            List<VacancyReport> reports =
                    vacancyReportRepository
                            .findByVacancyOrderByReportedAtDesc(vacancy);

            for (VacancyReport report : reports) {
                if (report.getStatus() == VacancyReport.Status.PENDING) {
                    report.setStatus(VacancyReport.Status.ACTION_TAKEN);
                    report.setReviewedAt(LocalDateTime.now());
                    vacancyReportRepository.save(report);
                }
            }

            try {
                emailService.sendVacancyRemovedNotification(vacancy);
            } catch (Exception ignored) {
            }
        }

        return "redirect:/admin/vacancies";
    }

    @PostMapping("/vacancy-reports/{reportId}/dismiss")
    public String dismissVacancyReport(
            @PathVariable Long reportId) {

        VacancyReport report = findReport(reportId);

        if (report.getStatus() == VacancyReport.Status.PENDING) {
            report.setStatus(VacancyReport.Status.DISMISSED);
            report.setReviewedAt(LocalDateTime.now());
            vacancyReportRepository.save(report);
        }

        return "redirect:/admin/vacancies";
    }

    @PostMapping("/vacancy-reports/{reportId}/warn")
    public String warnReportedUser(
            @PathVariable Long reportId) {

        VacancyReport report = findReport(reportId);

        if (report.getStatus() == VacancyReport.Status.PENDING) {
            report.setStatus(VacancyReport.Status.ACTION_TAKEN);
            report.setReviewedAt(LocalDateTime.now());
            vacancyReportRepository.save(report);

            try {
                emailService.sendVacancyModerationWarning(report);
            } catch (Exception ignored) {
            }
        }

        return "redirect:/admin/vacancies";
    }

    @PostMapping("/vacancy-reports/{reportId}/remove")
    public String removeReportedVacancy(
            @PathVariable Long reportId) {

        VacancyReport report = findReport(reportId);
        Vacancy vacancy = report.getVacancy();

        if (!vacancy.isRemoved()) {
            vacancy.setRemoved(true);
            vacancy.setStatus(Vacancy.Status.CLOSED);
            vacancyRepository.save(vacancy);

            List<VacancyReport> reports =
                    vacancyReportRepository
                            .findByVacancyOrderByReportedAtDesc(vacancy);

            LocalDateTime now = LocalDateTime.now();

            for (VacancyReport item : reports) {
                if (item.getStatus() == VacancyReport.Status.PENDING) {
                    item.setStatus(VacancyReport.Status.ACTION_TAKEN);
                    item.setReviewedAt(now);
                    vacancyReportRepository.save(item);
                }
            }

            try {
                emailService.sendVacancyRemovedNotification(vacancy);
            } catch (Exception ignored) {
            }
        }

        return "redirect:/admin/vacancies";
    }

    private void addAdminNavigation(Model model) {

        long pendingPatterns =
                patternRepository.findByApprovalStatus(
                        Pattern.ApprovalStatus.PENDING
                ).size();

        long pendingReports =
                vacancyReportRepository.countByStatus(
                        VacancyReport.Status.PENDING
                );

        long pendingPatternReports =
                patternReportRepository.countByStatus(
                        PatternReport.Status.PENDING
                );

        model.addAttribute(
                "moderationNotificationCount",
                pendingPatterns + pendingReports + pendingPatternReports
        );
    }

    private Withdrawal findWithdrawal(Long id) {
        return withdrawalRepository.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Withdrawal not found"
                        )
                );
    }

    private Vacancy findVacancy(Long id) {
        return vacancyRepository.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Vacancy not found"
                        )
                );
    }

    private PatternReport findPatternReport(Long id) {
        return patternReportRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Pattern report not found"));
    }

    private VacancyReport findReport(Long id) {
        return vacancyReportRepository.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Vacancy report not found"
                        )
                );
    }
}
