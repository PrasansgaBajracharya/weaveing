package com.weaveing.security;

import com.weaveing.service.UserService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class LoginAuthenticationFailureHandler
        implements AuthenticationFailureHandler {

    private final UserService userService;

    public LoginAuthenticationFailureHandler(UserService userService) {
        this.userService = userService;
    }

    @Override
    public void onAuthenticationFailure(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException exception)
            throws IOException, ServletException {

        if (exception instanceof DisabledException) {
            String login = request.getParameter("username");

            if (userService.resendVerificationEmail(login)) {
                response.sendRedirect("/login?verification=pending");
                return;
            }
        }

        response.sendRedirect("/login?error");
    }
}
