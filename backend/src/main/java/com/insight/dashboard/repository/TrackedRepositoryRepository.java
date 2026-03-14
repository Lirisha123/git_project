package com.insight.dashboard.repository;

import com.insight.dashboard.entity.TrackedRepository;
import java.util.Optional;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TrackedRepositoryRepository extends JpaRepository<TrackedRepository, Long> {
    Optional<TrackedRepository> findByFullNameIgnoreCaseAndUserId(String fullName, Long userId);
    Optional<TrackedRepository> findByIdAndUserId(Long id, Long userId);
    List<TrackedRepository> findAllByUserId(Long userId);
}
