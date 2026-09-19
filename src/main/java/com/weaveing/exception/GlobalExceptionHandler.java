package com.weaveing.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public Object handleIllegalArgument(
            IllegalArgumentException exception,
            HttpServletRequest request) {

        return handleException(
                exception,
                request,
                HttpStatus.BAD_REQUEST
        );
    }

    @ExceptionHandler(IllegalStateException.class)
    public Object handleIllegalState(
            IllegalStateException exception,
            HttpServletRequest request) {

        return handleException(
                exception,
                request,
                HttpStatus.INTERNAL_SERVER_ERROR
        );
    }

    @ExceptionHandler(Exception.class)
    public Object handleGeneralException(
            Exception exception,
            HttpServletRequest request) {

        return handleException(
                exception,
                request,
                HttpStatus.INTERNAL_SERVER_ERROR
        );
    }

    private Object handleException(
            Exception exception,
            HttpServletRequest request,
            HttpStatus status) {

        String path = request.getRequestURI();

        if (path.startsWith("/api/")) {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("timestamp", LocalDateTime.now());
            body.put("status", status.value());
            body.put("error", status.getReasonPhrase());
            body.put("message", exception.getMessage());
            body.put("path", path);

            return ResponseEntity
                    .status(status)
                    .body(body);
        }

        ModelAndView modelAndView = new ModelAndView("error");
        modelAndView.setStatus(status);
        modelAndView.addObject("status", status.value());
        modelAndView.addObject("error", status.getReasonPhrase());
        modelAndView.addObject(
                "message",
                exception.getMessage() != null
                        ? exception.getMessage()
                        : "Something went wrong. Please try again."
        );

        return modelAndView;
    }
}
