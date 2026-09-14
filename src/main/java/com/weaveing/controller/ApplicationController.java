package com.weaveing.controller;

import com.weaveing.entity.Application;
import com.weaveing.entity.User;
import com.weaveing.entity.Vacancy;
import com.weaveing.repository.ApplicationRepository;
import com.weaveing.repository.UserRepository;
import com.weaveing.repository.VacancyRepository;
import com.weaveing.service.EmailService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/vacancies")
public class ApplicationController {

    private final VacancyRepository vacancyRepository;
    private final ApplicationRepository applicationRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;

    public ApplicationController(
            VacancyRepository vacancyRepository,
            ApplicationRepository applicationRepository,
            UserRepository userRepository,
            EmailService emailService) {

        this.vacancyRepository = vacancyRepository;
        this.applicationRepository = applicationRepository;
        this.userRepository = userRepository;
        this.emailService = emailService;
    }

    @GetMapping("/{vacancyId}/apply")
    public String applyForm(
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

        if (vacancy.getStatus() != Vacancy.Status.OPEN) {
            return "redirect:/vacancies/" + vacancyId;
        }

        if (applicationRepository.existsByVacancyAndApplicant(
                vacancy,
                user
        )) {
            return "redirect:/vacancies/" + vacancyId
                    + "?application=already";
        }

        Application application =
                new Application();

        application.setVacancy(vacancy);
        application.setApplicant(user);

        model.addAttribute("user", user);
        model.addAttribute("vacancy", vacancy);
        model.addAttribute("application", application);

        return "application-form";
    }

    @PostMapping("/{vacancyId}/apply")
    public String submitApplication(
            @PathVariable Long vacancyId,
            Authentication authentication,
            @ModelAttribute("application") Application application,
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

        if (vacancy.getStatus() != Vacancy.Status.OPEN) {
            return "redirect:/vacancies/" + vacancyId;
        }

        if (applicationRepository.existsByVacancyAndApplicant(
                vacancy,
                user
        )) {
            return "redirect:/vacancies/" + vacancyId
                    + "?application=already";
        }

        String message =
                application.getMessage();

        if (message == null ||
                message.trim().isEmpty()) {

            model.addAttribute("user", user);
            model.addAttribute("vacancy", vacancy);
            model.addAttribute("application", application);

            model.addAttribute(
                    "error",
                    "Please add a short message with your application."
            );

            return "application-form";
        }

        application.setVacancy(vacancy);
        application.setApplicant(user);
        application.setMessage(message.trim());
        application.setStatus(
                Application.Status.PENDING
        );

        applicationRepository.save(application);

        try {
            emailService.sendNewVacancyApplicationEmail(
                    application
            );
        } catch (Exception ignored) {
        }

        return "redirect:/vacancies/" + vacancyId
                + "?application=submitted";
    }

    @PostMapping(
            "/{vacancyId}/applications/{applicationId}/accept"
    )
    public String acceptApplication(
            @PathVariable Long vacancyId,
            @PathVariable Long applicationId,
            Authentication authentication) {

        User user =
                getCurrentUser(authentication);

        Application application =
                applicationRepository.findById(applicationId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Application not found"
                                )
                        );

        Vacancy vacancy =
                application.getVacancy();

        if (!vacancy.getId().equals(vacancyId)) {

            throw new IllegalArgumentException(
                    "Application does not belong to this vacancy"
            );
        }

        if (!vacancy.getPostedBy()
                .getId()
                .equals(user.getId())) {

            return "redirect:/vacancies/" + vacancyId;
        }

        application.setStatus(
                Application.Status.ACCEPTED
        );

        applicationRepository.save(application);

        try {
            emailService.sendVacancyApplicationAcceptedEmail(
                    application
            );
        } catch (Exception ignored) {
        }

        return "redirect:/vacancies/" + vacancyId;
    }

    @PostMapping(
            "/{vacancyId}/applications/{applicationId}/reject"
    )
    public String rejectApplication(
            @PathVariable Long vacancyId,
            @PathVariable Long applicationId,
            Authentication authentication) {

        User user =
                getCurrentUser(authentication);

        Application application =
                applicationRepository.findById(applicationId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Application not found"
                                )
                        );

        Vacancy vacancy =
                application.getVacancy();

        if (!vacancy.getId().equals(vacancyId)) {

            throw new IllegalArgumentException(
                    "Application does not belong to this vacancy"
            );
        }

        if (!vacancy.getPostedBy()
                .getId()
                .equals(user.getId())) {

            return "redirect:/vacancies/" + vacancyId;
        }

        application.setStatus(
                Application.Status.REJECTED
        );

        applicationRepository.save(application);

        try {
            emailService.sendVacancyApplicationRejectedEmail(
                    application
            );
        } catch (Exception ignored) {
        }

        return "redirect:/vacancies/" + vacancyId;
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