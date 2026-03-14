package com.insight.dashboard.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.insight.dashboard.exception.AppException;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class GitHubClient {

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String apiBaseUrl;
    private final String token;

    public GitHubClient(ObjectMapper objectMapper,
                        @Value("${app.github.api-base-url}") String apiBaseUrl,
                        @Value("${app.github.token:}") String token) {
        this.objectMapper = objectMapper;
        this.apiBaseUrl = apiBaseUrl;
        this.token = token;
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(20))
            .build();
    }

    public RepoSnapshot fetchRepositorySnapshot(String owner, String repositoryName) {
        try {
            JsonNode repo = getJson("/repos/" + encode(owner) + "/" + encode(repositoryName));
            Map<String, Long> languages = getLanguages(owner, repositoryName);
            List<ContributorSnapshot> contributors = getContributors(owner, repositoryName);
            List<CommitSnapshot> commits = getCommits(owner, repositoryName);
            String readme = getReadme(owner, repositoryName);

            return new RepoSnapshot(
                repo.path("name").asText(repositoryName),
                repo.path("full_name").asText(owner + "/" + repositoryName),
                repo.path("html_url").asText(),
                emptyToNull(repo.path("description").asText()),
                emptyToNull(repo.path("homepage").asText()),
                repo.path("default_branch").asText(),
                repo.path("visibility").asText("public"),
                emptyToNull(repo.path("language").asText()),
                repo.path("stargazers_count").asInt(),
                repo.path("forks_count").asInt(),
                repo.path("open_issues_count").asInt(),
                repo.path("subscribers_count").asInt(),
                objectMapper.convertValue(repo.path("topics"), new TypeReference<List<String>>() {}),
                languages,
                contributors,
                commits,
                readme
            );
        } catch (IOException | InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new AppException("Unable to contact GitHub API. Check repository name, rate limits, or network access.", exception);
        }
    }

    private Map<String, Long> getLanguages(String owner, String repositoryName) throws IOException, InterruptedException {
        JsonNode languages = getJson("/repos/" + encode(owner) + "/" + encode(repositoryName) + "/languages");
        Map<String, Long> response = new LinkedHashMap<>();
        languages.fields().forEachRemaining(entry -> response.put(entry.getKey(), entry.getValue().asLong()));
        return response;
    }

    private List<ContributorSnapshot> getContributors(String owner, String repositoryName) throws IOException, InterruptedException {
        JsonNode contributors = getJson("/repos/" + encode(owner) + "/" + encode(repositoryName) + "/contributors?per_page=8");
        List<ContributorSnapshot> response = new ArrayList<>();
        for (JsonNode contributor : contributors) {
            response.add(new ContributorSnapshot(
                contributor.path("login").asText(),
                contributor.path("contributions").asInt(),
                contributor.path("avatar_url").asText(),
                contributor.path("html_url").asText()
            ));
        }
        response.sort(Comparator.comparingInt(ContributorSnapshot::contributions).reversed());
        return response;
    }

    private List<CommitSnapshot> getCommits(String owner, String repositoryName) throws IOException, InterruptedException {
        String since = DateTimeFormatter.ISO_INSTANT.format(Instant.now().minus(Duration.ofDays(56)));
        JsonNode commits = getJson("/repos/" + encode(owner) + "/" + encode(repositoryName) + "/commits?per_page=100&since=" + encode(since));
        List<CommitSnapshot> response = new ArrayList<>();
        for (JsonNode commit : commits) {
            String committedAt = commit.path("commit").path("author").path("date").asText();
            if (!committedAt.isBlank()) {
                response.add(new CommitSnapshot(Instant.parse(committedAt)));
            }
        }
        return response;
    }

    private String getReadme(String owner, String repositoryName) {
        try {
            JsonNode readme = getJson("/repos/" + encode(owner) + "/" + encode(repositoryName) + "/readme");
            String content = readme.path("content").asText();
            if (content.isBlank()) {
                return null;
            }
            return new String(java.util.Base64.getMimeDecoder().decode(content), StandardCharsets.UTF_8);
        } catch (Exception exception) {
            return null;
        }
    }

    private JsonNode getJson(String path) throws IOException, InterruptedException {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
            .uri(URI.create(apiBaseUrl + path))
            .header("Accept", "application/vnd.github+json")
            .header("X-GitHub-Api-Version", "2022-11-28")
            .timeout(Duration.ofSeconds(30))
            .GET();

        if (token != null && !token.isBlank()) {
            builder.header("Authorization", "Bearer " + token);
        }

        HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 400) {
            throw new AppException("GitHub API returned status " + response.statusCode() + ": " + response.body());
        }
        return objectMapper.readTree(response.body());
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private String emptyToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    public record RepoSnapshot(
        String repositoryName,
        String fullName,
        String githubUrl,
        String description,
        String homepage,
        String defaultBranch,
        String visibility,
        String primaryLanguage,
        int stars,
        int forks,
        int openIssues,
        int watchers,
        List<String> topics,
        Map<String, Long> languages,
        List<ContributorSnapshot> contributors,
        List<CommitSnapshot> commits,
        String readme
    ) {}

    public record ContributorSnapshot(String login, int contributions, String avatarUrl, String profileUrl) {}

    public record CommitSnapshot(Instant committedAt) {
        public LocalDate weekBucket() {
            LocalDate date = committedAt.atZone(ZoneOffset.UTC).toLocalDate();
            return date.minusDays(date.getDayOfWeek().getValue() - 1L);
        }
    }
}
