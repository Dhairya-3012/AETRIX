package com.aetrix.repository;

import com.aetrix.entity.LlmSummaryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LlmSummaryRepository extends JpaRepository<LlmSummaryEntity, Long> {

    // City-filtered queries
    Optional<LlmSummaryEntity> findByCityAndFeatureKey(String city, String featureKey);

    List<LlmSummaryEntity> findByCity(String city);

    void deleteByCityAndFeatureKey(String city, String featureKey);

    void deleteByCity(String city);

    // Legacy queries
    Optional<LlmSummaryEntity> findByFeatureKey(String featureKey);

    void deleteByFeatureKey(String featureKey);
}
