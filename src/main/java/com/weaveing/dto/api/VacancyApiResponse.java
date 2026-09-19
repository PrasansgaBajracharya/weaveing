package com.weaveing.dto.api;

import com.weaveing.entity.Vacancy;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record VacancyApiResponse(
        Long id,
        String title,
        String description,
        String requirements,
        LocalDate deadline,
        String postedByUsername,
        Vacancy.Status status,
        LocalDateTime createdAt
) {

    public static VacancyApiResponse from(Vacancy vacancy) {
        return new VacancyApiResponse(
                vacancy.getId(),
                vacancy.getTitle(),
                vacancy.getDescription(),
                vacancy.getRequirements(),
                vacancy.getDeadline(),
                vacancy.getPostedBy() == null
                        ? null
                        : vacancy.getPostedBy().getUsername(),
                vacancy.getStatus(),
                vacancy.getCreatedAt()
        );
    }
}
