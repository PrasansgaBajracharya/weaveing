package com.weaveing.repository;

import com.weaveing.entity.Pattern;
import com.weaveing.entity.PatternReport;
import com.weaveing.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PatternReportRepository extends JpaRepository<PatternReport, Long> {

    List<PatternReport> findByStatusOrderByReportedAtDesc(PatternReport.Status status);

    List<PatternReport> findAllByOrderByReportedAtDesc();

    List<PatternReport> findByPatternOrderByReportedAtDesc(Pattern pattern);

    boolean existsByPatternAndReportedBy(Pattern pattern, User reportedBy);

    long countByStatus(PatternReport.Status status);
}
