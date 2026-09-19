package com.weaveing.service.api;

import com.weaveing.dto.api.VacancyApiResponse;
import com.weaveing.entity.Vacancy;
import com.weaveing.repository.VacancyRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class VacancyApiService {

    private final VacancyRepository vacancyRepository;

    public VacancyApiService(VacancyRepository vacancyRepository) {
        this.vacancyRepository = vacancyRepository;
    }

    public List<VacancyApiResponse> getOpenVacancies() {
        return vacancyRepository
                .findByRemovedFalseOrderByCreatedAtDesc()
                .stream()
                .filter(vacancy -> vacancy.getStatus() == Vacancy.Status.OPEN)
                .map(VacancyApiResponse::from)
                .toList();
    }

    public VacancyApiResponse getOpenVacancy(Long id) {
        return vacancyRepository.findById(id)
                .filter(vacancy ->
                        !vacancy.isRemoved()
                                && vacancy.getStatus() == Vacancy.Status.OPEN
                )
                .map(VacancyApiResponse::from)
                .orElse(null);
    }
}
