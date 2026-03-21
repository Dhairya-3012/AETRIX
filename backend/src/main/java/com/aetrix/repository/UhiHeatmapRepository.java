package com.aetrix.repository;

import com.aetrix.entity.UhiHeatmapPointEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UhiHeatmapRepository extends JpaRepository<UhiHeatmapPointEntity, Long> {

    List<UhiHeatmapPointEntity> findByIsAnomalyTrue();

    long countByIsAnomalyTrue();

    List<UhiHeatmapPointEntity> findBySeverity(String severity);

    long countBySeverity(String severity);

    @Query("SELECT AVG(u.lstCelsius) FROM UhiHeatmapPointEntity u")
    Double findCityMeanLst();

    @Query("SELECT MAX(u.lstCelsius) FROM UhiHeatmapPointEntity u")
    Double findCityMaxLst();

    @Query("SELECT MIN(u.lstCelsius) FROM UhiHeatmapPointEntity u")
    Double findCityMinLst();

    @Query("SELECT u FROM UhiHeatmapPointEntity u ORDER BY u.lstCelsius DESC")
    List<UhiHeatmapPointEntity> findAllOrderByLstDesc();
}
