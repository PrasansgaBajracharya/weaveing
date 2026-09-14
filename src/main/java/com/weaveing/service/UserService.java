package com.weaveing.service;

import com.weaveing.dto.SignupRequest;
import com.weaveing.entity.TrustedDevice;
import com.weaveing.entity.User;
import com.weaveing.repository.TrustedDeviceRepository;
import com.weaveing.repository.UserRepository;
import com.weaveing.security.LoginAuthenticationSuccessHandler;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final TrustedDeviceRepository trustedDeviceRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    public UserService(
            UserRepository userRepository,
            TrustedDeviceRepository trustedDeviceRepository,
            PasswordEncoder passwordEncoder,
            EmailService emailService) {

        this.userRepository = userRepository;
        this.trustedDeviceRepository = trustedDeviceRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
    }

    public String registerUser(SignupRequest request) {

        String email =
                request.getEmail()
                        .trim()
                        .toLowerCase();

        String username =
                request.getUsername()
                        .trim();

        if (userRepository.findByEmail(email).isPresent()) {
            return "Email already exists";
        }

        if (userRepository.findByUsername(username).isPresent()) {
            return "Username already exists";
        }

        if (!request.getPassword()
                .equals(request.getConfirmPassword())) {

            return "Passwords do not match";
        }

        User user =
                new User();

        user.setName(
                request.getName().trim()
        );

        user.setUsername(username);
        user.setEmail(email);

        user.setPassword(
                passwordEncoder.encode(
                        request.getPassword()
                )
        );

        String token =
                UUID.randomUUID().toString();

        user.setVerificationToken(token);
        user.setVerified(false);

        userRepository.save(user);

        emailService.sendVerificationEmail(
                user.getEmail(),
                token
        );

        return "Registration successful";
    }

    public boolean resendVerificationEmail(
            String login) {

        if (login == null ||
                login.isBlank()) {

            return false;
        }

        User user =
                userRepository
                        .findByEmail(
                                login.trim().toLowerCase()
                        )
                        .orElseGet(() ->
                                userRepository
                                        .findByUsername(
                                                login.trim()
                                        )
                                        .orElse(null)
                        );

        if (user == null ||
                user.isVerified()) {

            return false;
        }

        String token =
                UUID.randomUUID().toString();

        user.setVerificationToken(token);

        userRepository.save(user);

        emailService.sendVerificationEmail(
                user.getEmail(),
                token
        );

        return true;
    }

    public User verifyUser(
            String token) {

        User user =
                userRepository
                        .findByVerificationToken(token)
                        .orElse(null);

        if (user == null) {
            return null;
        }

        user.setVerified(true);
        user.setVerificationToken(null);

        return userRepository.save(user);
    }

    public String getTrustedDeviceCookie(
            HttpServletRequest request,
            String login) {

        if (login == null ||
                login.isBlank()) {

            return null;
        }

        User user = findUser(login);

        if (user == null ||
                user.getId() == null) {

            return null;
        }

        String cookieName =
                LoginAuthenticationSuccessHandler
                        .trustedDeviceCookieName(
                                user.getId()
                        );

        Cookie[] cookies =
                request.getCookies();

        if (cookies == null) {
            return null;
        }

        for (Cookie cookie : cookies) {

            if (cookieName.equals(
                    cookie.getName()
            )) {

                return cookie.getValue();
            }
        }

        return null;
    }

    public boolean isTrustedDevice(
            String login,
            String token) {

        if (login == null ||
                login.isBlank() ||
                token == null ||
                token.isBlank()) {

            return false;
        }

        User user =
                findUser(login);

        if (user == null) {
            return false;
        }

        TrustedDevice device =
                trustedDeviceRepository
                        .findByToken(token)
                        .orElse(null);

        if (device == null ||
                !device.isVerified() ||
                device.getUser() == null ||
                device.getUser().getId() == null ||
                !device.getUser()
                        .getId()
                        .equals(user.getId())) {

            return false;
        }

        if (device.getExpiresAt()
                .isBefore(LocalDateTime.now())) {

            trustedDeviceRepository.delete(device);

            return false;
        }

        return true;
    }

    public boolean createDeviceVerification(
            String login,
            boolean rememberDevice) {

        User user =
                findUser(login);

        if (user == null ||
                !user.isVerified()) {

            return false;
        }

        TrustedDevice device =
                new TrustedDevice();

        device.setUser(user);

        device.setToken(
                UUID.randomUUID()
                        .toString()
                        .replace("-", "")
        );

        device.setExpiresAt(
                LocalDateTime.now()
                        .plusMinutes(15)
        );

        device.setVerified(false);

        trustedDeviceRepository.save(device);

        emailService.sendDeviceVerificationEmail(
                user.getEmail(),
                device.getToken(),
                rememberDevice
        );

        return true;
    }

    public User verifyDevice(
            String token,
            boolean rememberDevice) {

        if (token == null ||
                token.isBlank()) {

            return null;
        }

        TrustedDevice device =
                trustedDeviceRepository
                        .findByToken(token)
                        .orElse(null);

        if (device == null ||
                device.isVerified() ||
                device.getExpiresAt()
                        .isBefore(LocalDateTime.now())) {

            return null;
        }

        device.setVerified(true);

        if (rememberDevice) {

            device.setExpiresAt(
                    LocalDateTime.now()
                            .plusDays(30)
            );

        } else {

            device.setExpiresAt(
                    LocalDateTime.now()
                            .plusMinutes(2)
            );
        }

        trustedDeviceRepository.save(device);

        return device.getUser();
    }

    private User findUser(
            String login) {

        if (login == null ||
                login.isBlank()) {

            return null;
        }

        return userRepository
                .findByUsername(login.trim())
                .orElseGet(() ->
                        userRepository
                                .findByEmail(
                                        login.trim()
                                                .toLowerCase()
                                )
                                .orElse(null)
                );
    }
}