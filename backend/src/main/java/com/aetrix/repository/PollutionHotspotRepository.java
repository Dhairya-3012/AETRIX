package com.aetrix.repository;

import com.aetrix.entity.PollutionHotspotEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PollutionHotspotRepository extends JpaRepository<PollutionHotspotEntity, Long> {

    // City-filtered queries
    List<PollutionHotspotEntity> findByCity(String city);

    List<PollutionHotspotEntity> findByCityAndSeverity(String city, String severity);

    List<PollutionHotspotEntity> findByCityOrderByAvgRiskScoreDesc(String city);

    long countByCity(String city);

    void deleteByCity(String city);

    // Legacy queries
    List<PollutionHotspotEntity> findBySeverity(String severity);

    List<PollutionHotspotEntity> findAllByOrderByAvgRiskScoreDesc();
}
