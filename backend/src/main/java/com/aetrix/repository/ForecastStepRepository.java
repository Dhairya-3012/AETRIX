package com.aetrix.repository;

import com.aetrix.entity.ForecastStepEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ForecastStepRepository extends JpaRepository<ForecastStepEntity, Long> {

    // City-filtered queries
    List<ForecastStepEntity> findByCity(String city);

    List<ForecastStepEntity> findByCityAndStepTypeOrderByStepAsc(String city, String stepType);

    long countByCityAndStepType(String city, String stepType);

    long countByCity(String city);

    @Query("SELECT AVG(f.predictedCelsius) FROM ForecastStepEntity f WHERE f.city = :city AND f.stepType = 'predicted'")
    Double findPredictedMean(@Param("city") String city);

    @Query("SELECT MIN(f.lowerBound) FROM ForecastStepEntity f WHERE f.city = :city AND f.stepType = 'predicted'")
    Double findPredictedMin(@Param("city") String city);

    @Query("SELECT MAX(f.upperBound) FROM ForecastStepEntity f WHERE f.city = :city AND f.stepType = 'predicted'")
    Double findPredictedMax(@Param("city") String city);

    void deleteByCity(String city);

    // Legacy queries
    List<ForecastStepEntity> findByStepTypeOrderByStepAsc(String stepType);

    @Query("SELECT AVG(f.predictedCelsius) FROM ForecastStepEntity f WHERE f.stepType = 'predicted'")
    Double findPredictedMean();

    @Query("SELECT MIN(f.lowerBound) FROM ForecastStepEntity f WHERE f.stepType = 'predicted'")
    Double findPredictedMin();

    @Query("SELECT MAX(f.upperBound) FROM ForecastStepEntity f WHERE f.stepType = 'predicted'")
    Double findPredictedMax();

    long countByStepType(String stepType);
}
