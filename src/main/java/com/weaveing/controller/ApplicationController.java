package com.weaveing.controller;

import com.weaveing.entity.Application;
import com.weaveing.entity.User;
import com.weaveing.entity.Vacancy;
import com.weaveing.repository.ApplicationRepository;
import com.weaveing.repository.UserRepository;
import com.weaveing.repository.VacancyRepository;
import com.weaveing.service.NotificationService;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;

@Controller
@RequestMapping("/vacancies")
public class ApplicationController {

    private final VacancyRepository vacancyRepository;
    private final ApplicationRepository applicationRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    public ApplicationController(
            VacancyRepository vacancyRepository,
            ApplicationRepository applicationRepository,
            UserRepository userRepository,
            NotificationService notificationService) {

        this.vacancyRepository = vacancyRepository;
        this.applicationRepository = applicationRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
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
            @RequestParam("cv") MultipartFile cv,
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

        String cvError = validateCv(cv);

        if (cvError != null) {
            model.addAttribute("user", user);
            model.addAttribute("vacancy", vacancy);
            model.addAttribute("application", application);
            model.addAttribute("cvError", cvError);
            return "application-form";
        }

        application.setVacancy(vacancy);
        application.setApplicant(user);
        application.setMessage(message.trim());

        try {
            application.setCvData(cv.getBytes());
        } catch (Exception e) {
            model.addAttribute("user", user);
            model.addAttribute("vacancy", vacancy);
            model.addAttribute("application", application);
            model.addAttribute(
                    "cvError",
                    "We could not read that CV. Please choose the PDF again."
            );
            return "application-form";
        }

        application.setCvFileName(sanitizeFileName(cv.getOriginalFilename()));
        application.setCvContentType("application/pdf");
        application.setStatus(
                Application.Status.PENDING
        );

        applicationRepository.save(application);

        notificationService.notifyNewVacancyApplication(application);

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

        notificationService.notifyVacancyApplicationAccepted(application);

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

        notificationService.notifyVacancyApplicationRejected(application);

        return "redirect:/vacancies/" + vacancyId;
    }

    @GetMapping("/{vacancyId}/applications/{applicationId}/cv")
    @Transactional(readOnly = true)
    public ResponseEntity<ByteArrayResource> downloadCv(
            @PathVariable Long vacancyId,
            @PathVariable Long applicationId,
            Authentication authentication) {

        User user = getCurrentUser(authentication);

        Application application =
                applicationRepository.findById(applicationId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Application not found"
                                )
                        );

        Vacancy vacancy = application.getVacancy();

        if (vacancy == null ||
                vacancy.getId() == null ||
                !vacancy.getId().equals(vacancyId) ||
                vacancy.getPostedBy() == null ||
                !vacancy.getPostedBy().getId().equals(user.getId())) {

            return ResponseEntity.notFound().build();
        }

        if (application.getCvData() == null ||
                application.getCvData().length == 0) {

            return ResponseEntity.notFound().build();
        }

        String fileName =
                application.getCvFileName() != null &&
                        !application.getCvFileName().isBlank()
                        ? application.getCvFileName()
                        : "cv.pdf";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(
                ContentDisposition.attachment()
                        .filename(fileName, StandardCharsets.UTF_8)
                        .build()
        );

        headers.setCacheControl(CacheControl.noStore());

        return ResponseEntity.ok()
                .headers(headers)
                .body(new ByteArrayResource(application.getCvData()));
    }

    private String validateCv(MultipartFile cv) {

        if (cv == null || cv.isEmpty()) {
            return "Please upload your CV as a PDF.";
        }

        if (cv.getSize() > 5L * 1024 * 1024) {
            return "Your CV must be 5 MB or smaller.";
        }

        String fileName = cv.getOriginalFilename();
        String contentType = cv.getContentType();

        if (fileName == null ||
                !fileName.toLowerCase().endsWith(".pdf") ||
                !"application/pdf".equalsIgnoreCase(contentType)) {
            return "Please upload a PDF CV only.";
        }

        try {
            byte[] bytes = cv.getBytes();

            if (bytes.length < 4 ||
                    bytes[0] != '%' ||
                    bytes[1] != 'P' ||
                    bytes[2] != 'D' ||
                    bytes[3] != 'F') {
                return "The uploaded file is not a valid PDF.";
            }

        } catch (Exception e) {
            return "We could not validate that CV. Please choose the PDF again.";
        }

        return null;
    }

    private String sanitizeFileName(String fileName) {

        if (fileName == null || fileName.isBlank()) {
            return "cv.pdf";
        }

        String safeName =
                fileName.replace("\\", "/");

        int slash = safeName.lastIndexOf('/');

        if (slash >= 0) {
            safeName = safeName.substring(slash + 1);
        }

        safeName =
                safeName.replaceAll("[^a-zA-Z0-9._-]", "_");

        return safeName.isBlank() ? "cv.pdf" : safeName;
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