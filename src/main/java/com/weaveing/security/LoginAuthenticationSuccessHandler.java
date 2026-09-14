package com.weaveing.security;

import com.weaveing.service.UserService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class LoginAuthenticationSuccessHandler
        implements AuthenticationSuccessHandler {

    public static final String TRUSTED_DEVICE_COOKIE_PREFIX =
            "WEAVE_TRUSTED_DEVICE_";

    public static final String DEVICE_VERIFIED_ONCE =
            "DEVICE_VERIFIED_ONCE";

    private final UserService userService;

    public LoginAuthenticationSuccessHandler(
            UserService userService) {

        this.userService = userService;
    }

    public static String trustedDeviceCookieName(Long userId) {

        return TRUSTED_DEVICE_COOKIE_PREFIX + userId;
    }

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication)
            throws IOException, ServletException {

        var session =
                request.getSession(false);

        if (session != null) {

            Object verifiedOnce =
                    session.getAttribute(
                            DEVICE_VERIFIED_ONCE
                    );

            if (Boolean.TRUE.equals(verifiedOnce)) {

                session.removeAttribute(
                        DEVICE_VERIFIED_ONCE
                );

                response.sendRedirect(
                        authentication.getAuthorities().stream()
                                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()))
                                ? "/admin"
                                : "/home"
                );
                return;
            }
        }

        String token =
                userService.getTrustedDeviceCookie(
                        request,
                        authentication.getName()
                );

        if (userService.isTrustedDevice(
                authentication.getName(),
                token)) {

            response.sendRedirect(
                    authentication.getAuthorities().stream()
                            .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()))
                            ? "/admin"
                            : "/home"
            );
            return;
        }

        boolean rememberDevice =
                "on".equalsIgnoreCase(
                        request.getParameter("rememberDevice")
                );

        if (userService.createDeviceVerification(
                authentication.getName(),
                rememberDevice)) {

            SecurityContextHolder.clearContext();

            if (session != null) {
                session.invalidate();
            }

            response.sendRedirect(
                    "/login?verification=device"
            );

            return;
        }

        response.sendRedirect("/login?error");
    }
}