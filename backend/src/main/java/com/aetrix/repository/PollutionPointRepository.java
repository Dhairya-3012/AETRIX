package com.aetrix.repository;

import com.aetrix.entity.PollutionPointEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PollutionPointRepository extends JpaRepository<PollutionPointEntity, Long> {

    List<PollutionPointEntity> findByRiskCategory(String riskCategory);

    List<PollutionPointEntity> findByIsExtremeOutlierTrue();

    long countByRiskCategory(String riskCategory);

    long countByIsExtremeOutlierTrue();

    @Query("SELECT AVG(p.riskScore) FROM PollutionPointEntity p")
    Double findCityMeanRisk();

    @Query("SELECT MAX(p.riskScore) FROM PollutionPointEntity p")
    Double findCityMaxRisk();

    @Query("SELECT p FROM PollutionPointEntity p ORDER BY p.riskScore DESC")
    List<PollutionPointEntity> findAllOrderByRiskScoreDesc();
}
