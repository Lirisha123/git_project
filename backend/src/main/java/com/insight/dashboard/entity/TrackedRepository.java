package com.insight.dashboard.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "tracked_repositories")
public class TrackedRepository {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private String owner;

    @Column(nullable = false)
    private String repositoryName;

    @Column(nullable = false, unique = true)
    private String fullName;

    private String alias;

    @Column(length = 2000)
    private String notes;

    private String githubUrl;
    @Lob
    private String description;

    @Lob
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

    @Lob
    private String topicsJson;

    @Lob
    private String languagesJson;

    @Lob
    private String commitTimelineJson;

    @Lob
    private String contributorActivityJson;

    @Lob
    private String techStackJson;

    @Lob
    private String improvementSuggestionsJson;

    @Column(length = 4000)
    private String aiPurpose;

    @Column(length = 1000)
    private String aiComplexity;

    @Column(length = 2000)
    private String aiOverview;

    private LocalDateTime lastSyncedAt;
    private LocalDateTime lastAnalyzedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
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
    public String getTopicsJson() { return topicsJson; }
    public void setTopicsJson(String topicsJson) { this.topicsJson = topicsJson; }
    public String getLanguagesJson() { return languagesJson; }
    public void setLanguagesJson(String languagesJson) { this.languagesJson = languagesJson; }
    public String getCommitTimelineJson() { return commitTimelineJson; }
    public void setCommitTimelineJson(String commitTimelineJson) { this.commitTimelineJson = commitTimelineJson; }
    public String getContributorActivityJson() { return contributorActivityJson; }
    public void setContributorActivityJson(String contributorActivityJson) { this.contributorActivityJson = contributorActivityJson; }
    public String getTechStackJson() { return techStackJson; }
    public void setTechStackJson(String techStackJson) { this.techStackJson = techStackJson; }
    public String getImprovementSuggestionsJson() { return improvementSuggestionsJson; }
    public void setImprovementSuggestionsJson(String improvementSuggestionsJson) { this.improvementSuggestionsJson = improvementSuggestionsJson; }
    public String getAiPurpose() { return aiPurpose; }
    public void setAiPurpose(String aiPurpose) { this.aiPurpose = aiPurpose; }
    public String getAiComplexity() { return aiComplexity; }
    public void setAiComplexity(String aiComplexity) { this.aiComplexity = aiComplexity; }
    public String getAiOverview() { return aiOverview; }
    public void setAiOverview(String aiOverview) { this.aiOverview = aiOverview; }
    public LocalDateTime getLastSyncedAt() { return lastSyncedAt; }
    public void setLastSyncedAt(LocalDateTime lastSyncedAt) { this.lastSyncedAt = lastSyncedAt; }
    public LocalDateTime getLastAnalyzedAt() { return lastAnalyzedAt; }
    public void setLastAnalyzedAt(LocalDateTime lastAnalyzedAt) { this.lastAnalyzedAt = lastAnalyzedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
