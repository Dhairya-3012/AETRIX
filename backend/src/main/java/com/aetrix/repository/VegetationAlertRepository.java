package com.aetrix.repository;

import com.aetrix.entity.VegetationAlertEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;


@Repository
public interface VegetationAlertRepository extends JpaRepository<VegetationAlertEntity, Long> {

    // City-filtered queries
    List<VegetationAlertEntity> findByCity(String city);

    List<VegetationAlertEntity> findByCityAndSeverity(String city, String severity);

    long countByCityAndSeverity(String city, String severity);

    long countByCity(String city);

    @Query("SELECT v FROM VegetationAlertEntity v WHERE v.city = :city ORDER BY v.zScore ASC")
    List<VegetationAlertEntity> findByCityOrderByZScoreDesc(@Param("city") String city);

    void deleteByCity(String city);

    // Legacy queries
    List<VegetationAlertEntity> findBySeverity(String severity);

    @Query("SELECT v FROM VegetationAlertEntity v ORDER BY v.zScore ASC")
    List<VegetationAlertEntity> findAllOrderByZScoreDesc();

    long countBySeverity(String severity);
}
