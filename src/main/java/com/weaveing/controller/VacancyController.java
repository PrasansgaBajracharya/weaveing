package com.weaveing.controller;

import com.weaveing.entity.Application;
import com.weaveing.entity.User;
import com.weaveing.entity.Vacancy;
import com.weaveing.repository.ApplicationRepository;
import com.weaveing.repository.UserRepository;
import com.weaveing.repository.VacancyRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@Controller
@RequestMapping("/vacancies")
public class VacancyController {

    private final VacancyRepository vacancyRepository;
    private final ApplicationRepository applicationRepository;
    private final UserRepository userRepository;

    public VacancyController(
            VacancyRepository vacancyRepository,
            ApplicationRepository applicationRepository,
            UserRepository userRepository) {

        this.vacancyRepository = vacancyRepository;
        this.applicationRepository = applicationRepository;
        this.userRepository = userRepository;
    }

    @GetMapping
    public String vacancies(
            Authentication authentication,
            Model model) {

        User user = getCurrentUser(authentication);

        model.addAttribute("user", user);

        model.addAttribute(
                "vacancies",
                vacancyRepository.findByRemovedFalseOrderByCreatedAtDesc()
        );

        return "vacancies";
    }

    @GetMapping("/new")
    public String newVacancy(
            Authentication authentication,
            Model model) {

        User user = getCurrentUser(authentication);

        model.addAttribute("user", user);
        model.addAttribute("vacancy", new Vacancy());

        return "vacancy-form";
    }

    @PostMapping
    public String createVacancy(
            Authentication authentication,
            @ModelAttribute("vacancy") Vacancy vacancy,
            BindingResult bindingResult,
            Model model) {

        User user = getCurrentUser(authentication);

        if (vacancy.getTitle() == null ||
                vacancy.getTitle().trim().isEmpty()) {

            bindingResult.rejectValue(
                    "title",
                    "required",
                    "Please enter a vacancy title."
            );
        }

        if (vacancy.getDescription() == null ||
                vacancy.getDescription().trim().isEmpty()) {

            bindingResult.rejectValue(
                    "description",
                    "required",
                    "Please enter a description."
            );
        }

        if (vacancy.getRequirements() == null ||
                vacancy.getRequirements().trim().isEmpty()) {

            bindingResult.rejectValue(
                    "requirements",
                    "required",
                    "Please enter the requirements."
            );
        }

        if (vacancy.getDeadline() == null) {

            bindingResult.rejectValue(
                    "deadline",
                    "required",
                    "Please select a deadline."
            );

        } else if (vacancy.getDeadline().isBefore(LocalDate.now())) {

            bindingResult.rejectValue(
                    "deadline",
                    "past",
                    "Deadline cannot be in the past."
            );
        }

        if (bindingResult.hasErrors()) {

            model.addAttribute("user", user);

            return "vacancy-form";
        }

        vacancy.setTitle(
                vacancy.getTitle().trim()
        );

        vacancy.setDescription(
                vacancy.getDescription().trim()
        );

        vacancy.setRequirements(
                vacancy.getRequirements().trim()
        );

        vacancy.setPostedBy(user);
        vacancy.setStatus(Vacancy.Status.OPEN);

        if (vacancy.getCreatedAt() == null) {

            vacancy.setCreatedAt(
                    java.time.LocalDateTime.now()
            );
        }

        vacancyRepository.save(vacancy);

        return "redirect:/vacancies/" + vacancy.getId();
    }

    @GetMapping("/{id}")
    public String vacancyDetails(
            @PathVariable Long id,
            Authentication authentication,
            Model model) {

        User user = getCurrentUser(authentication);

        Vacancy vacancy =
                vacancyRepository.findById(id)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Vacancy not found"
                                )
                        );

        boolean owner =
                vacancy.getPostedBy()
                        .getId()
                        .equals(user.getId());

        model.addAttribute("user", user);
        model.addAttribute("vacancy", vacancy);
        model.addAttribute("owner", owner);

        if (owner) {

            model.addAttribute(
                    "applications",
                    applicationRepository
                            .findByVacancyOrderByAppliedAtDesc(
                                    vacancy
                            )
            );

        } else {

            Application existingApplication =
                    applicationRepository
                            .findByVacancyAndApplicant(
                                    vacancy,
                                    user
                            );

            model.addAttribute(
                    "existingApplication",
                    existingApplication
            );
        }

        return "vacancy-details";
    }

    @PostMapping("/{vacancyId}/close")
    public String closeVacancy(
            @PathVariable Long vacancyId,
            Authentication authentication) {

        User user = getCurrentUser(authentication);

        Vacancy vacancy =
                vacancyRepository.findById(vacancyId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Vacancy not found"
                                )
                        );

        if (!vacancy.getPostedBy().getId().equals(user.getId())) {
            return "redirect:/vacancies/" + vacancyId;
        }

        vacancy.setStatus(Vacancy.Status.CLOSED);

        vacancyRepository.save(vacancy);

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