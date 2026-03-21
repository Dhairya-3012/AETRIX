package com.aetrix.repository;

import com.aetrix.entity.VegetationPointEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VegetationPointRepository extends JpaRepository<VegetationPointEntity, Long> {

    List<VegetationPointEntity> findByHealthLabel(String healthLabel);

    long countByHealthLabel(String healthLabel);

    @Query("SELECT AVG(v.ndviSentinel) FROM VegetationPointEntity v")
    Double findCityMeanNdvi();

    @Query("SELECT AVG(v.ndviLandsat) FROM VegetationPointEntity v")
    Double findCityMeanNdviLandsat();

    @Query("SELECT v FROM VegetationPointEntity v ORDER BY v.ndviSentinel ASC")
    List<VegetationPointEntity> findAllOrderByNdviAsc();
}
