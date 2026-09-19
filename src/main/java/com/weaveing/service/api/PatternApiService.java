package com.weaveing.service.api;

import com.weaveing.dto.api.PatternApiResponse;
import com.weaveing.entity.Pattern;
import com.weaveing.repository.PatternRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PatternApiService {

    private final PatternRepository patternRepository;

    public PatternApiService(PatternRepository patternRepository) {
        this.patternRepository = patternRepository;
    }

    public List<PatternApiResponse> getApprovedPatterns() {
        return patternRepository
                .findByApprovalStatus(Pattern.ApprovalStatus.APPROVED)
                .stream()
                .filter(pattern -> pattern.getRemovedAt() == null)
                .map(PatternApiResponse::from)
                .toList();
    }

    public PatternApiResponse getApprovedPattern(Long id) {
        return patternRepository.findById(id)
                .filter(pattern ->
                        pattern.getApprovalStatus() == Pattern.ApprovalStatus.APPROVED
                                && pattern.getRemovedAt() == null
                )
                .map(PatternApiResponse::from)
                .orElse(null);
    }
}
