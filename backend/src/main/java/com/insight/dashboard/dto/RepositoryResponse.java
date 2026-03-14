package com.insight.dashboard.dto;

import java.time.LocalDateTime;
import java.util.List;

public class RepositoryResponse {

    private Long id;
    private String owner;
    private String repositoryName;
    private String fullName;
    private String alias;
    private String notes;
    private String githubUrl;
    private String description;
    private String homepage;
    private String defaultBranch;
    private String visibility;
    private String primaryLanguage;
    private Integer stars;
    private Integer forks;
    private Integer openIssues;
    private Integer watchers;
    private Integer contributorCount;
    private Double commitVelocity;
    private String healthAssessment;
    private List<String> topics;
    private List<LanguageShare> languages;
    private List<CommitPoint> commitTimeline;
    private List<ContributorStat> contributors;
    private InsightResponse insight;
    private LocalDateTime lastSyncedAt;
    private LocalDateTime lastAnalyzedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static class LanguageShare {
        private String name;
        private double percentage;

        public LanguageShare() {}
        public LanguageShare(String name, double percentage) {
            this.name = name;
            this.percentage = percentage;
        }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public double getPercentage() { return percentage; }
        public void setPercentage(double percentage) { this.percentage = percentage; }
    }

    public static class CommitPoint {
        private String label;
        private int commits;

        public CommitPoint() {}
        public CommitPoint(String label, int commits) {
            this.label = label;
            this.commits = commits;
        }
        public String getLabel() { return label; }
        public void setLabel(String label) { this.label = label; }
        public int getCommits() { return commits; }
        public void setCommits(int commits) { this.commits = commits; }
    }

    public static class ContributorStat {
        private String login;
        private int contributions;
        private String avatarUrl;
        private String profileUrl;

        public ContributorStat() {}
        public ContributorStat(String login, int contributions, String avatarUrl, String profileUrl) {
            this.login = login;
            this.contributions = contributions;
            this.avatarUrl = avatarUrl;
            this.profileUrl = profileUrl;
        }
        public String getLogin() { return login; }
        public void setLogin(String login) { this.login = login; }
        public int getContributions() { return contributions; }
        public void setContributions(int contributions) { this.contributions = contributions; }
        public String getAvatarUrl() { return avatarUrl; }
        public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
        public String getProfileUrl() { return profileUrl; }
        public void setProfileUrl(String profileUrl) { this.profileUrl = profileUrl; }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getOwner() { return owner; }
    public void setOwner(String owner) { this.owner = owner; }
    public String getRepositoryName() { return repositoryName; }
    public void setRepositoryName(String repositoryName) { this.repositoryName = repositoryName; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getAlias() { return alias; }
    public void setAlias(String alias) { this.alias = alias; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public String getGithubUrl() { return githubUrl; }
    public void setGithubUrl(String githubUrl) { this.githubUrl = githubUrl; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getHomepage() { return homepage; }
    public void setHomepage(String homepage) { this.homepage = homepage; }
    public String getDefaultBranch() { return defaultBranch; }
    public void setDefaultBranch(String defaultBranch) { this.defaultBranch = defaultBranch; }
    public String getVisibility() { return visibility; }
    public void setVisibility(String visibility) { this.visibility = visibility; }
    public String getPrimaryLanguage() { return primaryLanguage; }
    public void setPrimaryLanguage(String primaryLanguage) { this.primaryLanguage = primaryLanguage; }
    public Integer getStars() { return stars; }
    public void setStars(Integer stars) { this.stars = stars; }
    public Integer getForks() { return forks; }
    public void setForks(Integer forks) { this.forks = forks; }
    public Integer getOpenIssues() { return openIssues; }
    public void setOpenIssues(Integer openIssues) { this.openIssues = openIssues; }
    public Integer getWatchers() { return watchers; }
    public void setWatchers(Integer watchers) { this.watchers = watchers; }
    public Integer getContributorCount() { return contributorCount; }
    public void setContributorCount(Integer contributorCount) { this.contributorCount = contributorCount; }
    public Double getCommitVelocity() { return commitVelocity; }
    public void setCommitVelocity(Double commitVelocity) { this.commitVelocity = commitVelocity; }
    public String getHealthAssessment() { return healthAssessment; }
    public void setHealthAssessment(String healthAssessment) { this.healthAssessment = healthAssessment; }
    public List<String> getTopics() { return topics; }
    public void setTopics(List<String> topics) { this.topics = topics; }
    public List<LanguageShare> getLanguages() { return languages; }
    public void setLanguages(List<LanguageShare> languages) { this.languages = languages; }
    public List<CommitPoint> getCommitTimeline() { return commitTimeline; }
    public void setCommitTimeline(List<CommitPoint> commitTimeline) { this.commitTimeline = commitTimeline; }
    public List<ContributorStat> getContributors() { return contributors; }
    public void setContributors(List<ContributorStat> contributors) { this.contributors = contributors; }
    public InsightResponse getInsight() { return insight; }
    public void setInsight(InsightResponse insight) { this.insight = insight; }
    public LocalDateTime getLastSyncedAt() { return lastSyncedAt; }
    public void setLastSyncedAt(LocalDateTime lastSyncedAt) { this.lastSyncedAt = lastSyncedAt; }
    public LocalDateTime getLastAnalyzedAt() { return lastAnalyzedAt; }
    public void setLastAnalyzedAt(LocalDateTime lastAnalyzedAt) { this.lastAnalyzedAt = lastAnalyzedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
