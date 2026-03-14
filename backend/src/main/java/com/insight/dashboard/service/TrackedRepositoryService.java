package com.insight.dashboard.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.insight.dashboard.dto.DashboardSummaryResponse;
import com.insight.dashboard.dto.InsightResponse;
import com.insight.dashboard.dto.RepositoryRequest;
import com.insight.dashboard.dto.RepositoryResponse;
import com.insight.dashboard.entity.TrackedRepository;
import com.insight.dashboard.exception.BadRequestException;
import com.insight.dashboard.exception.NotFoundException;
import com.insight.dashboard.repository.TrackedRepositoryRepository;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class TrackedRepositoryService {

    private final TrackedRepositoryRepository repository;
    private final GitHubClient gitHubClient;
    private final InsightComposer insightComposer;
    private final ObjectMapper objectMapper;

    public TrackedRepositoryService(TrackedRepositoryRepository repository,
                                    GitHubClient gitHubClient,
                                    InsightComposer insightComposer,
                                    ObjectMapper objectMapper) {
        this.repository = repository;
        this.gitHubClient = gitHubClient;
        this.insightComposer = insightComposer;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public List<RepositoryResponse> findAll(Long userId) {
        return repository.findAllByUserId(userId).stream()
            .sorted(Comparator.comparing(TrackedRepository::getUpdatedAt).reversed())
            .map(this::toResponse)
            .toList();
    }

    @Transactional(readOnly = true)
    public RepositoryResponse findById(Long id, Long userId) {
        return toResponse(getRepository(id, userId));
    }

    public RepositoryResponse create(RepositoryRequest request, Long userId) {
        validateRequest(request);
        String fullName = fullName(request.getOwner(), request.getRepositoryName());
        repository.findByFullNameIgnoreCaseAndUserId(fullName, userId).ifPresent(existing -> {
            throw new BadRequestException("Repository is already being tracked: " + fullName);
        });

        TrackedRepository trackedRepository = new TrackedRepository();
        trackedRepository.setUserId(userId);
        trackedRepository.setOwner(request.getOwner().trim());
        trackedRepository.setRepositoryName(request.getRepositoryName().trim());
        trackedRepository.setFullName(fullName);
        trackedRepository.setAlias(blankToNull(request.getAlias()));
        trackedRepository.setNotes(blankToNull(request.getNotes()));

        syncIntoEntity(trackedRepository);
        return toResponse(repository.save(trackedRepository));
    }

    public RepositoryResponse update(Long id, RepositoryRequest request, Long userId) {
        validateRequest(request);
        TrackedRepository trackedRepository = getRepository(id, userId);
        String fullName = fullName(request.getOwner(), request.getRepositoryName());
        repository.findByFullNameIgnoreCaseAndUserId(fullName, userId)
            .filter(existing -> !existing.getId().equals(id))
            .ifPresent(existing -> {
                throw new BadRequestException("Another tracked entry already uses " + fullName);
            });

        trackedRepository.setOwner(request.getOwner().trim());
        trackedRepository.setRepositoryName(request.getRepositoryName().trim());
        trackedRepository.setFullName(fullName);
        trackedRepository.setAlias(blankToNull(request.getAlias()));
        trackedRepository.setNotes(blankToNull(request.getNotes()));
        syncIntoEntity(trackedRepository);
        return toResponse(repository.save(trackedRepository));
    }

    public RepositoryResponse sync(Long id, Long userId) {
        TrackedRepository trackedRepository = getRepository(id, userId);
        syncIntoEntity(trackedRepository);
        return toResponse(repository.save(trackedRepository));
    }

    public RepositoryResponse analyze(Long id, Long userId) {
        TrackedRepository trackedRepository = getRepository(id, userId);
        if (trackedRepository.getLastSyncedAt() == null) {
            syncIntoEntity(trackedRepository);
        }
        InsightComposer.AnalyticsBundle bundle = insightComposer.compose(gitHubClient.fetchRepositorySnapshot(
            trackedRepository.getOwner(), trackedRepository.getRepositoryName()));
        applyInsights(trackedRepository, bundle);
        trackedRepository.setLastAnalyzedAt(LocalDateTime.now());
        return toResponse(repository.save(trackedRepository));
    }

    public void delete(Long id, Long userId) {
        repository.delete(getRepository(id, userId));
    }

    @Transactional(readOnly = true)
    public DashboardSummaryResponse buildSummary(Long userId) {
        List<TrackedRepository> repositories = repository.findAllByUserId(userId);
        DashboardSummaryResponse response = new DashboardSummaryResponse();
        response.setTrackedRepositories(repositories.size());
        response.setTotalStars(repositories.stream().map(TrackedRepository::getStars).filter(java.util.Objects::nonNull).mapToInt(Integer::intValue).sum());
        response.setTotalForks(repositories.stream().map(TrackedRepository::getForks).filter(java.util.Objects::nonNull).mapToInt(Integer::intValue).sum());
        response.setAverageWeeklyCommits((int) Math.round(repositories.stream().map(TrackedRepository::getCommitVelocity).filter(java.util.Objects::nonNull).mapToDouble(Double::doubleValue).average().orElse(0)));
        response.setTotalContributors(repositories.stream().map(TrackedRepository::getContributorCount).filter(java.util.Objects::nonNull).mapToInt(Integer::intValue).sum());
        response.setHealthiestRepository(repositories.stream()
            .sorted(Comparator.comparing(TrackedRepository::getHealthAssessment, Comparator.nullsLast(String::compareTo)).reversed()
                .thenComparing(TrackedRepository::getStars, Comparator.nullsLast(Integer::compareTo)).reversed())
            .map(TrackedRepository::getFullName)
            .findFirst()
            .orElse("No repositories yet"));
        return response;
    }

    private void syncIntoEntity(TrackedRepository trackedRepository) {
        GitHubClient.RepoSnapshot snapshot = gitHubClient.fetchRepositorySnapshot(trackedRepository.getOwner(), trackedRepository.getRepositoryName());
        InsightComposer.AnalyticsBundle bundle = insightComposer.compose(snapshot);

        trackedRepository.setRepositoryName(snapshot.repositoryName());
        trackedRepository.setFullName(snapshot.fullName());
        trackedRepository.setGithubUrl(snapshot.githubUrl());
        trackedRepository.setDescription(snapshot.description());
        trackedRepository.setHomepage(snapshot.homepage());
        trackedRepository.setDefaultBranch(snapshot.defaultBranch());
        trackedRepository.setVisibility(snapshot.visibility());
        trackedRepository.setPrimaryLanguage(snapshot.primaryLanguage());
        trackedRepository.setStars(snapshot.stars());
        trackedRepository.setForks(snapshot.forks());
        trackedRepository.setOpenIssues(snapshot.openIssues());
        trackedRepository.setWatchers(snapshot.watchers());
        trackedRepository.setContributorCount(bundle.contributors().size());
        trackedRepository.setCommitVelocity(bundle.commitVelocity());
        trackedRepository.setHealthAssessment(bundle.healthAssessment());
        trackedRepository.setTopicsJson(writeValue(snapshot.topics()));
        trackedRepository.setLanguagesJson(writeValue(bundle.languages()));
        trackedRepository.setCommitTimelineJson(writeValue(bundle.commitTimeline()));
        trackedRepository.setContributorActivityJson(writeValue(bundle.contributors()));
        applyInsights(trackedRepository, bundle);
        trackedRepository.setLastSyncedAt(LocalDateTime.now());
        trackedRepository.setLastAnalyzedAt(LocalDateTime.now());
    }

    private void applyInsights(TrackedRepository trackedRepository, InsightComposer.AnalyticsBundle bundle) {
        trackedRepository.setAiOverview(bundle.insight().getOverview());
        trackedRepository.setAiPurpose(bundle.insight().getPurpose());
        trackedRepository.setAiComplexity(bundle.insight().getComplexity());
        trackedRepository.setTechStackJson(writeValue(bundle.insight().getTechnologies()));
        trackedRepository.setImprovementSuggestionsJson(writeValue(bundle.insight().getImprovements()));
    }

    private RepositoryResponse toResponse(TrackedRepository trackedRepository) {
        RepositoryResponse response = new RepositoryResponse();
        response.setId(trackedRepository.getId());
        response.setOwner(trackedRepository.getOwner());
        response.setRepositoryName(trackedRepository.getRepositoryName());
        response.setFullName(trackedRepository.getFullName());
        response.setAlias(trackedRepository.getAlias());
        response.setNotes(trackedRepository.getNotes());
        response.setGithubUrl(trackedRepository.getGithubUrl());
        response.setDescription(trackedRepository.getDescription());
        response.setHomepage(trackedRepository.getHomepage());
        response.setDefaultBranch(trackedRepository.getDefaultBranch());
        response.setVisibility(trackedRepository.getVisibility());
        response.setPrimaryLanguage(trackedRepository.getPrimaryLanguage());
        response.setStars(trackedRepository.getStars());
        response.setForks(trackedRepository.getForks());
        response.setOpenIssues(trackedRepository.getOpenIssues());
        response.setWatchers(trackedRepository.getWatchers());
        response.setContributorCount(trackedRepository.getContributorCount());
        response.setCommitVelocity(trackedRepository.getCommitVelocity());
        response.setHealthAssessment(trackedRepository.getHealthAssessment());
        response.setTopics(readValue(trackedRepository.getTopicsJson(), new TypeReference<List<String>>() {}));
        response.setLanguages(readValue(trackedRepository.getLanguagesJson(), new TypeReference<List<RepositoryResponse.LanguageShare>>() {}));
        response.setCommitTimeline(readValue(trackedRepository.getCommitTimelineJson(), new TypeReference<List<RepositoryResponse.CommitPoint>>() {}));
        response.setContributors(readValue(trackedRepository.getContributorActivityJson(), new TypeReference<List<RepositoryResponse.ContributorStat>>() {}));
        response.setInsight(toInsight(trackedRepository));
        response.setLastSyncedAt(trackedRepository.getLastSyncedAt());
        response.setLastAnalyzedAt(trackedRepository.getLastAnalyzedAt());
        response.setCreatedAt(trackedRepository.getCreatedAt());
        response.setUpdatedAt(trackedRepository.getUpdatedAt());
        return response;
    }

    private InsightResponse toInsight(TrackedRepository trackedRepository) {
        InsightResponse insight = new InsightResponse();
        insight.setOverview(trackedRepository.getAiOverview());
        insight.setPurpose(trackedRepository.getAiPurpose());
        insight.setComplexity(trackedRepository.getAiComplexity());
        insight.setTechnologies(readValue(trackedRepository.getTechStackJson(), new TypeReference<List<String>>() {}));
        insight.setImprovements(readValue(trackedRepository.getImprovementSuggestionsJson(), new TypeReference<List<String>>() {}));
        return insight;
    }

    private TrackedRepository getRepository(Long id, Long userId) {
        return repository.findByIdAndUserId(id, userId)
            .orElseThrow(() -> new NotFoundException("Repository not found: " + id));
    }

    private void validateRequest(RepositoryRequest request) {
        if (request.getOwner() == null || request.getOwner().isBlank() || request.getRepositoryName() == null || request.getRepositoryName().isBlank()) {
            throw new BadRequestException("owner and repositoryName are required");
        }
    }

    private String writeValue(Object value) {
        try {
            return objectMapper.writeValueAsString(value == null ? List.of() : value);
        } catch (JsonProcessingException exception) {
            throw new BadRequestException("Unable to serialize repository analytics");
        }
    }

    private <T> T readValue(String json, TypeReference<T> typeReference) {
        try {
            if (json == null || json.isBlank()) {
                return objectMapper.readValue("[]", typeReference);
            }
            return objectMapper.readValue(json, typeReference);
        } catch (JsonProcessingException exception) {
            throw new BadRequestException("Unable to deserialize repository analytics");
        }
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String fullName(String owner, String repositoryName) {
        return owner.trim() + "/" + repositoryName.trim();
    }
}
