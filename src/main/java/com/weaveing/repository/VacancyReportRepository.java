package com.weaveing.repository;

import com.weaveing.entity.User;
import com.weaveing.entity.Vacancy;
import com.weaveing.entity.VacancyReport;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VacancyReportRepository
        extends JpaRepository<VacancyReport, Long> {

    List<VacancyReport> findByStatusOrderByReportedAtDesc(
            VacancyReport.Status status
    );

    List<VacancyReport> findAllByOrderByReportedAtDesc();

    List<VacancyReport> findByVacancyOrderByReportedAtDesc(
            Vacancy vacancy
    );

    boolean existsByVacancyAndReportedBy(
            Vacancy vacancy,
            User reportedBy
    );

    long countByStatus(VacancyReport.Status status);
}
