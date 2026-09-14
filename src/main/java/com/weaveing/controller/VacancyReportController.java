package com.weaveing.controller;

import com.weaveing.entity.User;
import com.weaveing.entity.Vacancy;
import com.weaveing.entity.VacancyReport;
import com.weaveing.repository.UserRepository;
import com.weaveing.repository.VacancyReportRepository;
import com.weaveing.repository.VacancyRepository;
import com.weaveing.service.EmailService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/vacancies")
public class VacancyReportController {

    private final VacancyRepository vacancyRepository;
    private final VacancyReportRepository vacancyReportRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;

    public VacancyReportController(
            VacancyRepository vacancyRepository,
            VacancyReportRepository vacancyReportRepository,
            UserRepository userRepository,
            EmailService emailService) {

        this.vacancyRepository = vacancyRepository;
        this.vacancyReportRepository = vacancyReportRepository;
        this.userRepository = userRepository;
        this.emailService = emailService;
    }

    @GetMapping("/{vacancyId}/report")
    public String reportForm(
            @PathVariable Long vacancyId,
            Authentication authentication,
            Model model) {

        User user = getCurrentUser(authentication);

        Vacancy vacancy =
                vacancyRepository.findById(vacancyId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Vacancy not found"
                                )
                        );

        if (vacancy.getPostedBy().getId().equals(user.getId())) {
            return "redirect:/vacancies/" + vacancyId;
        }

        if (vacancyReportRepository.existsByVacancyAndReportedBy(
                vacancy,
                user
        )) {
            return "redirect:/vacancies/" + vacancyId
                    + "?report=already";
        }

        VacancyReport report = new VacancyReport();
        report.setVacancy(vacancy);
        report.setReportedBy(user);

        model.addAttribute("user", user);
        model.addAttribute("vacancy", vacancy);
        model.addAttribute("report", report);
        model.addAttribute("reasons", VacancyReport.Reason.values());

        return "vacancy-report";
    }

    @PostMapping("/{vacancyId}/report")
    public String submitReport(
            @PathVariable Long vacancyId,
            Authentication authentication,
            @ModelAttribute("report") VacancyReport report,
            Model model) {

        User user = getCurrentUser(authentication);

        Vacancy vacancy =
                vacancyRepository.findById(vacancyId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Vacancy not found"
                                )
                        );

        if (vacancy.getPostedBy().getId().equals(user.getId())) {
            return "redirect:/vacancies/" + vacancyId;
        }

        if (vacancyReportRepository.existsByVacancyAndReportedBy(
                vacancy,
                user
        )) {
            return "redirect:/vacancies/" + vacancyId
                    + "?report=already";
        }

        if (report.getReason() == null) {
            model.addAttribute("user", user);
            model.addAttribute("vacancy", vacancy);
            model.addAttribute("reasons", VacancyReport.Reason.values());
            model.addAttribute(
                    "error",
                    "Please choose a reason for reporting this vacancy."
            );
            return "vacancy-report";
        }

        String details = report.getDetails();

        if (details != null) {
            details = details.trim();

            if (details.length() > 1000) {
                model.addAttribute("user", user);
                model.addAttribute("vacancy", vacancy);
                model.addAttribute("reasons", VacancyReport.Reason.values());
                model.addAttribute(
                        "error",
                        "Additional details must be 1000 characters or less."
                );
                return "vacancy-report";
            }
        }

        report.setVacancy(vacancy);
        report.setReportedBy(user);
        report.setDetails(details);
        report.setStatus(VacancyReport.Status.PENDING);

        vacancyReportRepository.save(report);

        try {
            emailService.sendVacancyReportAdminNotification(report);
        } catch (Exception ignored) {
        }

        try {
            emailService.sendVacancyReportUserNotification(report);
        } catch (Exception ignored) {
        }

        return "redirect:/vacancies/" + vacancyId
                + "?report=submitted";
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
