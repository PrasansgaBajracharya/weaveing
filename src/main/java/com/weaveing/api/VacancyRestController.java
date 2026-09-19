package com.weaveing.api;

import com.weaveing.dto.api.VacancyApiResponse;
import com.weaveing.service.api.VacancyApiService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/vacancies")
public class VacancyRestController {

    private final VacancyApiService vacancyApiService;

    public VacancyRestController(VacancyApiService vacancyApiService) {
        this.vacancyApiService = vacancyApiService;
    }

    @GetMapping
    public ResponseEntity<List<VacancyApiResponse>> getVacancies() {
        return ResponseEntity.ok(vacancyApiService.getOpenVacancies());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getVacancy(@PathVariable Long id) {
        VacancyApiResponse vacancy = vacancyApiService.getOpenVacancy(id);

        if (vacancy == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "Open vacancy not found"));
        }

        return ResponseEntity.ok(vacancy);
    }
}
