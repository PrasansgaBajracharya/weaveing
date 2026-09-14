package com.weaveing.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(
            ResourceHandlerRegistry registry) {

        Path uploadDirectory =
                Paths.get("uploads")
                        .toAbsolutePath()
                        .normalize();

        Path profilePictureDirectory =
                uploadDirectory
                        .resolve("profile-pictures")
                        .normalize();

        Path patternImageDirectory =
                uploadDirectory
                        .resolve("pattern-images")
                        .normalize();

        Path legacyPatternImageDirectory =
                uploadDirectory
                        .resolve("patterns")
                        .normalize();

        registry.addResourceHandler(
                "/uploads/profile-pictures/**"
        ).addResourceLocations(
                profilePictureDirectory.toUri().toString()
        );

        registry.addResourceHandler(
                "/uploads/pattern-images/*.png",
                "/uploads/pattern-images/*.jpg",
                "/uploads/pattern-images/*.jpeg",
                "/uploads/pattern-images/*.webp",
                "/uploads/pattern-images/*.gif"
        ).addResourceLocations(
                patternImageDirectory.toUri().toString()
        );

        registry.addResourceHandler(
                "/uploads/patterns/*.png",
                "/uploads/patterns/*.jpg",
                "/uploads/patterns/*.jpeg",
                "/uploads/patterns/*.webp",
                "/uploads/patterns/*.gif"
        ).addResourceLocations(
                legacyPatternImageDirectory.toUri().toString()
        );
    }
}