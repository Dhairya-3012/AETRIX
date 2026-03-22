package com.aetrix.repository;

import com.aetrix.entity.PollutionPointEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PollutionPointRepository extends JpaRepository<PollutionPointEntity, Long> {

    // City-filtered queries
    List<PollutionPointEntity> findByCity(String city);

    List<PollutionPointEntity> findByCityAndRiskCategory(String city, String riskCategory);

    List<PollutionPointEntity> findByCityAndIsExtremeOutlierTrue(String city);

    long countByCityAndRiskCategory(String city, String riskCategory);

    long countByCityAndIsExtremeOutlierTrue(String city);

    long countByCity(String city);

    @Query("SELECT AVG(p.riskScore) FROM PollutionPointEntity p WHERE p.city = :city")
    Double findCityMeanRisk(@Param("city") String city);

    @Query("SELECT MAX(p.riskScore) FROM PollutionPointEntity p WHERE p.city = :city")
    Double findCityMaxRisk(@Param("city") String city);

    @Query("SELECT p FROM PollutionPointEntity p WHERE p.city = :city ORDER BY p.riskScore DESC")
    List<PollutionPointEntity> findByCityOrderByRiskScoreDesc(@Param("city") String city);

    void deleteByCity(String city);

    // Legacy queries
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
