package com.insight.dashboard.dto;

public class DashboardSummaryResponse {

    private long trackedRepositories;
    private int totalStars;
    private int totalForks;
    private int averageWeeklyCommits;
    private int totalContributors;
    private String healthiestRepository;

    public long getTrackedRepositories() { return trackedRepositories; }
    public void setTrackedRepositories(long trackedRepositories) { this.trackedRepositories = trackedRepositories; }
    public int getTotalStars() { return totalStars; }
    public void setTotalStars(int totalStars) { this.totalStars = totalStars; }
    public int getTotalForks() { return totalForks; }
    public void setTotalForks(int totalForks) { this.totalForks = totalForks; }
    public int getAverageWeeklyCommits() { return averageWeeklyCommits; }
    public void setAverageWeeklyCommits(int averageWeeklyCommits) { this.averageWeeklyCommits = averageWeeklyCommits; }
    public int getTotalContributors() { return totalContributors; }
    public void setTotalContributors(int totalContributors) { this.totalContributors = totalContributors; }
    public String getHealthiestRepository() { return healthiestRepository; }
    public void setHealthiestRepository(String healthiestRepository) { this.healthiestRepository = healthiestRepository; }
}
