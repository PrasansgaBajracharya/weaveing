package com.weaveing.api;

import com.weaveing.dto.LeaderboardEntry;
import com.weaveing.dto.api.LeaderboardApiResponse;
import com.weaveing.service.LeaderboardService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/leaderboard")
public class LeaderboardRestController {

    private final LeaderboardService leaderboardService;

    public LeaderboardRestController(LeaderboardService leaderboardService) {
        this.leaderboardService = leaderboardService;
    }

    @GetMapping
    public ResponseEntity<List<LeaderboardApiResponse>> getLeaderboard(
            @RequestParam(defaultValue = "creators") String type) {

        List<LeaderboardEntry> entries = switch (type) {
            case "sellers" -> leaderboardService.getTopSellers();
            case "downloads" -> leaderboardService.getMostDownloaded();
            default -> leaderboardService.getTopCreators();
        };

        List<LeaderboardApiResponse> response = new java.util.ArrayList<>();

        for (int i = 0; i < entries.size(); i++) {
            LeaderboardEntry entry = entries.get(i);

            response.add(
                    new LeaderboardApiResponse(
                            i + 1L,
                            entry.getUser().getId(),
                            entry.getUser().getName(),
                            entry.getUser().getUsername(),
                            entry.getUser().getProfileImagePath(),
                            entry.getUser().getProfileImageObjectPosition(),
                            entry.getScore(),
                            entry.getMetric()
                    )
            );
        }

        return ResponseEntity.ok(response);
    }
}
