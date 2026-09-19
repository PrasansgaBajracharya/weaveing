package com.weaveing.dto.api;

import com.weaveing.entity.User;

import java.time.LocalDateTime;

public record UserApiResponse(
        Long id,
        String name,
        String username,
        LocalDateTime createdAt,
        String profileImagePath,
        String bio
) {

    public static UserApiResponse from(User user) {
        return new UserApiResponse(
                user.getId(),
                user.getName(),
                user.getUsername(),
                user.getCreatedAt(),
                user.getProfileImagePath(),
                user.getBio()
        );
    }
}
