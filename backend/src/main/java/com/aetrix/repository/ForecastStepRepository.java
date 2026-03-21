package com.aetrix.repository;

import com.aetrix.entity.ForecastStepEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ForecastStepRepository extends JpaRepository<ForecastStepEntity, Long> {

    List<ForecastStepEntity> findByStepTypeOrderByStepAsc(String stepType);

    @Query("SELECT AVG(f.predictedCelsius) FROM ForecastStepEntity f WHERE f.stepType = 'predicted'")
    Double findPredictedMean();

    @Query("SELECT MIN(f.lowerBound) FROM ForecastStepEntity f WHERE f.stepType = 'predicted'")
    Double findPredictedMin();

    @Query("SELECT MAX(f.upperBound) FROM ForecastStepEntity f WHERE f.stepType = 'predicted'")
    Double findPredictedMax();

    long countByStepType(String stepType);
}
