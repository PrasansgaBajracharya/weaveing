package com.weaveing.controller;

import com.weaveing.entity.Pattern;
import com.weaveing.entity.PatternReport;
import com.weaveing.entity.User;
import com.weaveing.repository.PatternReportRepository;
import com.weaveing.repository.PatternRepository;
import com.weaveing.repository.UserRepository;
import com.weaveing.service.EmailService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/patterns")
public class PatternReportController {

    private final PatternRepository patternRepository;
    private final PatternReportRepository patternReportRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;

    public PatternReportController(
            PatternRepository patternRepository,
            PatternReportRepository patternReportRepository,
            UserRepository userRepository,
            EmailService emailService) {
        this.patternRepository = patternRepository;
        this.patternReportRepository = patternReportRepository;
        this.userRepository = userRepository;
        this.emailService = emailService;
    }

    @GetMapping("/{patternId}/report")
    public String reportForm(
            @PathVariable Long patternId,
            Authentication authentication,
            Model model) {

        User user = getCurrentUser(authentication);
        Pattern pattern = findPattern(patternId);

        if (pattern.getCreator().getId().equals(user.getId())) {
            return "redirect:/patterns/" + patternId;
        }

        if (patternReportRepository.existsByPatternAndReportedBy(pattern, user)) {
            return "redirect:/patterns/" + patternId + "?report=already";
        }

        PatternReport report = new PatternReport();
        report.setPattern(pattern);
        report.setReportedBy(user);

        model.addAttribute("user", user);
        model.addAttribute("pattern", pattern);
        model.addAttribute("report", report);
        model.addAttribute("reasons", PatternReport.Reason.values());

        return "pattern-report";
    }

    @PostMapping("/{patternId}/report")
    public String submitReport(
            @PathVariable Long patternId,
            Authentication authentication,
            @ModelAttribute("report") PatternReport report,
            Model model) {

        User user = getCurrentUser(authentication);
        Pattern pattern = findPattern(patternId);

        if (pattern.getCreator().getId().equals(user.getId())) {
            return "redirect:/patterns/" + patternId;
        }

        if (patternReportRepository.existsByPatternAndReportedBy(pattern, user)) {
            return "redirect:/patterns/" + patternId + "?report=already";
        }

        if (report.getReason() == null) {
            return renderError(model, user, pattern,
                    "Please choose a reason for reporting this pattern.");
        }

        String details = report.getDetails();
        if (details != null) {
            details = details.trim();
            if (details.length() > 1000) {
                return renderError(model, user, pattern,
                        "Additional details must be 1000 characters or less.");
            }
        }

        report.setPattern(pattern);
        report.setReportedBy(user);
        report.setDetails(details);
        report.setStatus(PatternReport.Status.PENDING);
        patternReportRepository.save(report);

        try {
            emailService.sendPatternReportAdminNotification(report);
        } catch (Exception ignored) {
        }

        try {
            emailService.sendPatternReportUserNotification(report);
        } catch (Exception ignored) {
        }

        return "redirect:/patterns/" + patternId + "?report=submitted";
    }

    private String renderError(Model model, User user, Pattern pattern, String error) {
        model.addAttribute("user", user);
        model.addAttribute("pattern", pattern);
        model.addAttribute("reasons", PatternReport.Reason.values());
        model.addAttribute("error", error);
        return "pattern-report";
    }

    private Pattern findPattern(Long id) {
        return patternRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Pattern not found"));
    }

    private User getCurrentUser(Authentication authentication) {
        return userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new IllegalStateException("Logged-in user not found"));
    }
}
