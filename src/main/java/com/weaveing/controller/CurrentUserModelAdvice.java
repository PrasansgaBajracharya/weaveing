package com.weaveing.controller;

import com.weaveing.entity.User;
import com.weaveing.repository.UserRepository;
import com.weaveing.repository.NotificationRepository;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class CurrentUserModelAdvice {

    private final UserRepository userRepository;
    private final NotificationRepository notificationRepository;

    public CurrentUserModelAdvice(
            UserRepository userRepository,
            NotificationRepository notificationRepository) {
        this.userRepository = userRepository;
        this.notificationRepository = notificationRepository;
    }

    @ModelAttribute("user")
    public User currentUser(Authentication authentication) {
        if (authentication == null
                || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getName())) {
            return null;
        }

        User user = userRepository.findByUsername(authentication.getName())
                .orElse(null);

        if (user != null) {
            return user;
        }

        return null;
    }
    @ModelAttribute("notificationUnreadCount")
    public long notificationUnreadCount(Authentication authentication) {
        User user = currentUser(authentication);
        return user == null ? 0 : notificationRepository.countByUserAndReadFalse(user);
    }
}
