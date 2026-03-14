package com.insight.dashboard.service;

import com.insight.dashboard.dto.InsightResponse;
import com.insight.dashboard.dto.RepositoryResponse;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class InsightComposer {

    public AnalyticsBundle compose(GitHubClient.RepoSnapshot snapshot) {
        List<RepositoryResponse.LanguageShare> languages = toLanguageShares(snapshot.languages());
        List<RepositoryResponse.CommitPoint> commitTimeline = toCommitTimeline(snapshot.commits());
        List<RepositoryResponse.ContributorStat> contributors = snapshot.contributors().stream()
            .map(item -> new RepositoryResponse.ContributorStat(item.login(), item.contributions(), item.avatarUrl(), item.profileUrl()))
            .toList();

        double commitVelocity = commitTimeline.stream().mapToInt(RepositoryResponse.CommitPoint::getCommits).average().orElse(0);
        String health = determineHealth(snapshot.stars(), snapshot.openIssues(), contributors.size(), commitVelocity);
        InsightResponse insight = buildInsight(snapshot, languages, commitVelocity, health);

        return new AnalyticsBundle(languages, commitTimeline, contributors, commitVelocity, health, insight);
    }

    private List<RepositoryResponse.LanguageShare> toLanguageShares(Map<String, Long> languageBytes) {
        long total = languageBytes.values().stream().mapToLong(Long::longValue).sum();
        if (total == 0) {
            return List.of();
        }

        return languageBytes.entrySet().stream()
            .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
            .map(entry -> new RepositoryResponse.LanguageShare(
                entry.getKey(),
                Math.round((entry.getValue() * 10000.0) / total) / 100.0
            ))
            .toList();
    }

    private List<RepositoryResponse.CommitPoint> toCommitTimeline(List<GitHubClient.CommitSnapshot> commits) {
        LocalDate currentWeek = LocalDate.now().minusDays(LocalDate.now().getDayOfWeek().getValue() - 1L);
        Map<LocalDate, Integer> buckets = new LinkedHashMap<>();
        for (int index = 7; index >= 0; index--) {
            buckets.put(currentWeek.minusWeeks(index), 0);
        }
        for (GitHubClient.CommitSnapshot commit : commits) {
            LocalDate bucket = commit.weekBucket();
            if (buckets.containsKey(bucket)) {
                buckets.put(bucket, buckets.get(bucket) + 1);
            }
        }
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM", Locale.ENGLISH);
        return buckets.entrySet().stream()
            .map(entry -> new RepositoryResponse.CommitPoint(entry.getKey().format(formatter), entry.getValue()))
            .toList();
    }

    private String determineHealth(int stars, int openIssues, int contributors, double commitVelocity) {
        int score = 0;
        if (stars >= 50) { score += 2; }
        if (contributors >= 3) { score += 2; }
        if (commitVelocity >= 4) { score += 2; }
        if (openIssues <= 25) { score += 1; }

        if (score >= 6) {
            return "High momentum";
        }
        if (score >= 4) {
            return "Stable growth";
        }
        return "Needs attention";
    }

    private InsightResponse buildInsight(GitHubClient.RepoSnapshot snapshot,
                                         List<RepositoryResponse.LanguageShare> languages,
                                         double commitVelocity,
                                         String health) {
        InsightResponse response = new InsightResponse();
        List<String> technologies = new ArrayList<>();
        languages.stream().limit(4).map(RepositoryResponse.LanguageShare::getName).forEach(technologies::add);
        snapshot.topics().stream().limit(4).forEach(topic -> {
            if (!technologies.contains(topic)) {
                technologies.add(topic);
            }
        });
        if (technologies.isEmpty() && snapshot.primaryLanguage() != null) {
            technologies.add(snapshot.primaryLanguage());
        }

        String purpose = snapshot.description() != null
            ? snapshot.description()
            : extractPurposeFromReadme(snapshot.readme());
        if (purpose == null || purpose.isBlank()) {
            purpose = "Repository metadata suggests a focused engineering project without an explicit summary in the public profile.";
        }

        String complexity = switch ((int) Math.round(commitVelocity)) {
            case 0, 1, 2 -> languages.size() <= 2 ? "Low complexity" : "Moderate complexity";
            case 3, 4, 5 -> contributorsSignal(snapshot.contributors().size(), languages.size());
            default -> "High complexity";
        };

        List<String> improvements = new ArrayList<>();
        if (snapshot.homepage() == null || snapshot.homepage().isBlank()) {
            improvements.add("Add a production homepage or live demo link to make repository output easier to validate.");
        }
        if (snapshot.contributors().size() <= 1) {
            improvements.add("Contributor activity is concentrated; document ownership, review flow, and onboarding steps.");
        }
        if (languages.size() > 3) {
            improvements.add("The stack spans multiple languages; split build responsibilities clearly and document service boundaries.");
        }
        if (snapshot.openIssues() > 25) {
            improvements.add("Issue volume is elevated; introduce prioritization labels and a visible maintenance cadence.");
        }
        if (improvements.isEmpty()) {
            improvements.add("Current signals look healthy; the next gain is stronger release notes and deployment observability.");
        }

        response.setOverview("""
            %s appears to be a %s repository with %s contributor engagement and an estimated %.1f commits per week over the last eight weeks.
            """.formatted(snapshot.fullName(), health.toLowerCase(Locale.ENGLISH), snapshot.contributors().size(), commitVelocity).trim());
        response.setPurpose(purpose);
        response.setComplexity(complexity);
        response.setTechnologies(technologies);
        response.setImprovements(improvements);
        return response;
    }

    private String extractPurposeFromReadme(String readme) {
        if (readme == null || readme.isBlank()) {
            return null;
        }
        return readme.lines()
            .map(String::trim)
            .filter(line -> !line.isBlank() && !line.startsWith("#") && !line.startsWith("!["))
            .findFirst()
            .orElse(null);
    }

    private String contributorsSignal(int contributors, int languages) {
        if (contributors >= 4 || languages >= 4) {
            return "High complexity";
        }
        return "Moderate complexity";
    }

    public record AnalyticsBundle(
        List<RepositoryResponse.LanguageShare> languages,
        List<RepositoryResponse.CommitPoint> commitTimeline,
        List<RepositoryResponse.ContributorStat> contributors,
        double commitVelocity,
        String healthAssessment,
        InsightResponse insight
    ) {}
}
