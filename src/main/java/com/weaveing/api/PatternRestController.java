package com.weaveing.api;

import com.weaveing.dto.api.PatternApiResponse;
import com.weaveing.service.api.PatternApiService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/patterns")
public class PatternRestController {

    private final PatternApiService patternApiService;

    public PatternRestController(PatternApiService patternApiService) {
        this.patternApiService = patternApiService;
    }

    @GetMapping
    public ResponseEntity<List<PatternApiResponse>> getPatterns() {
        return ResponseEntity.ok(patternApiService.getApprovedPatterns());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getPattern(@PathVariable Long id) {
        PatternApiResponse pattern = patternApiService.getApprovedPattern(id);

        if (pattern == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "Approved pattern not found"));
        }

        return ResponseEntity.ok(pattern);
    }
}
