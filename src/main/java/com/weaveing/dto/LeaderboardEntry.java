package com.weaveing.dto;

import com.weaveing.entity.User;

public class LeaderboardEntry {

    private final User user;
    private final long score;
    private final String metric;

    public LeaderboardEntry(
            User user,
            long score,
            String metric) {

        this.user = user;
        this.score = score;
        this.metric = metric;
    }

    public User getUser() {
        return user;
    }

    public long getScore() {
        return score;
    }

    public String getMetric() {
        return metric;
    }
}
