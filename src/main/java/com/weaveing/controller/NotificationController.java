package com.weaveing.controller;

import com.weaveing.entity.User;
import com.weaveing.service.NotificationService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
@RequestMapping("/notifications")
public class NotificationController {

    private final NotificationService notificationService;
    private final CurrentUserModelAdvice currentUserModelAdvice;

    public NotificationController(
            NotificationService notificationService,
            CurrentUserModelAdvice currentUserModelAdvice) {
        this.notificationService = notificationService;
        this.currentUserModelAdvice = currentUserModelAdvice;
    }

    @GetMapping
    public String notifications(
            Authentication authentication,
            Model model) {

        User user = currentUserModelAdvice.currentUser(authentication);
        model.addAttribute("notifications", notificationService.getForUser(user));
        return "notifications";
    }


    @GetMapping("/{id}/open")
    public String openNotification(
            @PathVariable Long id,
            Authentication authentication) {

        User user = currentUserModelAdvice.currentUser(authentication);
        return "redirect:" + notificationService.openNotification(id, user);
    }

    @PostMapping("/{id}/read")
    public String markRead(
            @PathVariable Long id,
            Authentication authentication) {

        User user = currentUserModelAdvice.currentUser(authentication);
        notificationService.markAsRead(id, user);
        return "redirect:/notifications";
    }

    @PostMapping("/read-all")
    public String markAllRead(Authentication authentication) {

        User user = currentUserModelAdvice.currentUser(authentication);
        notificationService.markAllAsRead(user);
        return "redirect:/notifications";
    }
}
