package com.aetrix.repository;

import com.aetrix.entity.PollutionHotspotEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PollutionHotspotRepository extends JpaRepository<PollutionHotspotEntity, Long> {

    List<PollutionHotspotEntity> findBySeverity(String severity);

    List<PollutionHotspotEntity> findAllByOrderByAvgRiskScoreDesc();
}
