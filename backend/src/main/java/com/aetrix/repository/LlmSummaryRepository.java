package com.aetrix.repository;

import com.aetrix.entity.LlmSummaryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LlmSummaryRepository extends JpaRepository<LlmSummaryEntity, Long> {

    Optional<LlmSummaryEntity> findByFeatureKey(String featureKey);

    void deleteByFeatureKey(String featureKey);
}
