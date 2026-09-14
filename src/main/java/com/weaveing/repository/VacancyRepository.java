package com.weaveing.repository;

import com.weaveing.entity.User;
import com.weaveing.entity.Vacancy;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VacancyRepository
        extends JpaRepository<Vacancy, Long> {

    List<Vacancy> findByStatusOrderByCreatedAtDesc(
            Vacancy.Status status
    );

    List<Vacancy> findAllByOrderByCreatedAtDesc();

    List<Vacancy> findByRemovedFalseOrderByCreatedAtDesc();

}
