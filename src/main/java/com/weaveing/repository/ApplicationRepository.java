package com.weaveing.repository;

import com.weaveing.entity.Application;
import com.weaveing.entity.User;
import com.weaveing.entity.Vacancy;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ApplicationRepository
        extends JpaRepository<Application, Long> {

    List<Application> findByVacancyOrderByAppliedAtDesc(
            Vacancy vacancy
    );

    List<Application> findByApplicantOrderByAppliedAtDesc(
            User applicant
    );

    boolean existsByVacancyAndApplicant(
            Vacancy vacancy,
            User applicant
    );

    Application findByVacancyAndApplicant(
            Vacancy vacancy,
            User applicant
    );
}