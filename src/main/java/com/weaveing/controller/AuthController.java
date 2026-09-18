package com.weaveing.controller;

import com.weaveing.dto.SignupRequest;
import com.weaveing.entity.User;
import com.weaveing.security.LoginAuthenticationSuccessHandler;
import com.weaveing.service.UserService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Map;

@Controller
public class AuthController {

    private final UserService userService;

    public AuthController(
            UserService userService) {

        this.userService = userService;
    }

    @GetMapping("/signup")
    public String showSignupForm(
            Model model) {

        model.addAttribute(
                "signupRequest",
                new SignupRequest()
        );

        return "auth";
    }

    @PostMapping("/signup")
    public String registerUser(
            @Valid @ModelAttribute("signupRequest")
            SignupRequest request,
            BindingResult result,
            Model model) {

        if (result.hasErrors()) {

            model.addAttribute(
                    "showSignup",
                    true
            );

            return "auth";
        }

        String message =
                userService.registerUser(request);

        if (!message.equals(
                "Registration successful")) {

            model.addAttribute(
                    "error",
                    message
            );

            model.addAttribute(
                    "showSignup",
                    true
            );

            return "auth";
        }

        model.addAttribute(
                "success",
                "Account created successfully! Please check your Gmail to verify your account."
        );

        model.addAttribute(
                "showSignup",
                true
        );

        return "auth";
    }

    @GetMapping("/login")
    public String showLoginPage() {
        return "auth";
    }

    @GetMapping("/verify")
    public String verifyAccount(
            @RequestParam("token")
            String token,
            Model model) {

        User user =
                userService.verifyUser(token);

        if (user == null) {

            return "redirect:/login?verified=failed";
        }

        return "redirect:/login?verified=success";
    }

    @GetMapping("/login/device-status")
    @ResponseBody
    public Map<String, Boolean> deviceStatus(
            @RequestParam("login") String login,
            HttpServletRequest request) {

        String token =
                userService.getTrustedDeviceCookie(
                        request,
                        login
                );

        return Map.of(
                "trusted",
                userService.isTrustedDevice(login, token)
        );
    }

    @GetMapping("/verify-device")
    public String verifyDevice(
            @RequestParam("token")
            String token,
            @RequestParam(
                    value = "remember",
                    defaultValue = "true"
            ) boolean rememberDevice,
            HttpServletRequest request,
            Model model,
            HttpServletResponse response) {

        User user =
                userService.verifyDevice(
                        token,
                        rememberDevice
                );

        if (user == null) {

            return "redirect:/login?verification=device-failed";
        }

        if (rememberDevice) {

            Cookie cookie =
                    new Cookie(
                            LoginAuthenticationSuccessHandler
                                    .trustedDeviceCookieName(
                                            user.getId()
                                    ),
                            token
                    );

            cookie.setHttpOnly(true);
            cookie.setSecure(false);
            cookie.setPath("/");
            cookie.setMaxAge(
                    60 * 60 * 24 * 30
            );

            response.addCookie(cookie);

        } else {

            request.getSession(true)
                    .setAttribute(
                            LoginAuthenticationSuccessHandler
                                    .DEVICE_VERIFIED_ONCE,
                            true
                    );
        }

        return "redirect:/login?verification=" +
                (rememberDevice
                        ? "device-verified"
                        : "device-verified-once");
    }
}