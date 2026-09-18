package com.weaveing.controller;

import com.weaveing.entity.Pattern;
import com.weaveing.repository.PatternRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
public class LandingController {

    private final PatternRepository patternRepository;

    public LandingController(
            PatternRepository patternRepository) {
        this.patternRepository = patternRepository;
    }

    @GetMapping("/")
    public String landingPage(Model model) {

        List<Pattern> communityPatterns =
                patternRepository
                        .findTop12ByApprovalStatusAndRemovedAtIsNullOrderBySubmittedAtDesc(
                                Pattern.ApprovalStatus.APPROVED
                        );

        model.addAttribute(
                "communityPatterns",
                communityPatterns
        );

        return "landingpage";
    }
}
