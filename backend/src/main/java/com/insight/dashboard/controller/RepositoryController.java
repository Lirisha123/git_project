package com.insight.dashboard.controller;

import com.insight.dashboard.dto.DashboardSummaryResponse;
import com.insight.dashboard.dto.RepositoryRequest;
import com.insight.dashboard.dto.RepositoryResponse;
import com.insight.dashboard.security.AuthPrincipal;
import com.insight.dashboard.service.TrackedRepositoryService;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class RepositoryController {

    private final TrackedRepositoryService trackedRepositoryService;

    public RepositoryController(TrackedRepositoryService trackedRepositoryService) {
        this.trackedRepositoryService = trackedRepositoryService;
    }

    @GetMapping("/repositories")
    public List<RepositoryResponse> listRepositories(@AuthenticationPrincipal AuthPrincipal principal) {
        return trackedRepositoryService.findAll(principal.userId());
    }

    @GetMapping("/repositories/{id}")
    public RepositoryResponse getRepository(@PathVariable Long id, @AuthenticationPrincipal AuthPrincipal principal) {
        return trackedRepositoryService.findById(id, principal.userId());
    }

    @PostMapping("/repositories")
    @ResponseStatus(HttpStatus.CREATED)
    public RepositoryResponse createRepository(@RequestBody RepositoryRequest request, @AuthenticationPrincipal AuthPrincipal principal) {
        return trackedRepositoryService.create(request, principal.userId());
    }

    @PutMapping("/repositories/{id}")
    public RepositoryResponse updateRepository(@PathVariable Long id, @RequestBody RepositoryRequest request, @AuthenticationPrincipal AuthPrincipal principal) {
        return trackedRepositoryService.update(id, request, principal.userId());
    }

    @PostMapping("/repositories/{id}/sync")
    public RepositoryResponse syncRepository(@PathVariable Long id, @AuthenticationPrincipal AuthPrincipal principal) {
        return trackedRepositoryService.sync(id, principal.userId());
    }

    @PostMapping("/repositories/{id}/analyze")
    public RepositoryResponse analyzeRepository(@PathVariable Long id, @AuthenticationPrincipal AuthPrincipal principal) {
        return trackedRepositoryService.analyze(id, principal.userId());
    }

    @DeleteMapping("/repositories/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteRepository(@PathVariable Long id, @AuthenticationPrincipal AuthPrincipal principal) {
        trackedRepositoryService.delete(id, principal.userId());
    }

    @GetMapping("/dashboard/summary")
    public DashboardSummaryResponse dashboardSummary(@AuthenticationPrincipal AuthPrincipal principal) {
        return trackedRepositoryService.buildSummary(principal.userId());
    }
}
