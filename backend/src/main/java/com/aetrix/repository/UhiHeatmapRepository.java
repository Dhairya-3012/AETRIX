package com.aetrix.repository;

import com.aetrix.entity.UhiHeatmapPointEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UhiHeatmapRepository extends JpaRepository<UhiHeatmapPointEntity, Long> {

    // City-filtered queries
    List<UhiHeatmapPointEntity> findByCity(String city);

    List<UhiHeatmapPointEntity> findByCityAndIsAnomalyTrue(String city);

    long countByCityAndIsAnomalyTrue(String city);

    List<UhiHeatmapPointEntity> findByCityAndSeverity(String city, String severity);

    long countByCityAndSeverity(String city, String severity);

    long countByCity(String city);

    @Query("SELECT AVG(u.lstCelsius) FROM UhiHeatmapPointEntity u WHERE u.city = :city")
    Double findCityMeanLst(@Param("city") String city);

    @Query("SELECT MAX(u.lstCelsius) FROM UhiHeatmapPointEntity u WHERE u.city = :city")
    Double findCityMaxLst(@Param("city") String city);

    @Query("SELECT MIN(u.lstCelsius) FROM UhiHeatmapPointEntity u WHERE u.city = :city")
    Double findCityMinLst(@Param("city") String city);

    @Query("SELECT u FROM UhiHeatmapPointEntity u WHERE u.city = :city ORDER BY u.lstCelsius DESC")
    List<UhiHeatmapPointEntity> findByCityOrderByLstDesc(@Param("city") String city);

    // Legacy queries (for backward compatibility)
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

    void deleteByCity(String city);
}
