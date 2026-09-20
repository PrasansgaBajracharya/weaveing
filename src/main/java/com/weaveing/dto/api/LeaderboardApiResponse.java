package com.weaveing.dto.api;

public class LeaderboardApiResponse {

    private final long rank;
    private final Long userId;
    private final String name;
    private final String username;
    private final String profileImagePath;
    private final String profileImageObjectPosition;
    private final long score;
    private final String metric;

    public LeaderboardApiResponse(
            long rank,
            Long userId,
            String name,
            String username,
            String profileImagePath,
            String profileImageObjectPosition,
            long score,
            String metric) {

        this.rank = rank;
        this.userId = userId;
        this.name = name;
        this.username = username;
        this.profileImagePath = profileImagePath;
        this.profileImageObjectPosition = profileImageObjectPosition;
        this.score = score;
        this.metric = metric;
    }

    public long getRank() {
        return rank;
    }

    public Long getUserId() {
        return userId;
    }

    public String getName() {
        return name;
    }

    public String getUsername() {
        return username;
    }

    public String getProfileImagePath() {
        return profileImagePath;
    }

    public String getProfileImageObjectPosition() {
        return profileImageObjectPosition;
    }

    public long getScore() {
        return score;
    }

    public String getMetric() {
        return metric;
    }
}
