package com.aetrix.repository;

import com.aetrix.entity.VegetationAlertEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;


@Repository
public interface VegetationAlertRepository extends JpaRepository<VegetationAlertEntity, Long> {
    List<VegetationAlertEntity> findBySeverity(String severity);

    @Query("SELECT v FROM VegetationAlertEntity v ORDER BY v.zScore ASC")
    List<VegetationAlertEntity> findAllOrderByZScoreDesc();

    long countBySeverity(String severity);
}
