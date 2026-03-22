package com.aetrix.repository;

import com.aetrix.entity.VegetationPointEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VegetationPointRepository extends JpaRepository<VegetationPointEntity, Long> {

    // City-filtered queries
    List<VegetationPointEntity> findByCity(String city);

    List<VegetationPointEntity> findByCityAndHealthLabel(String city, String healthLabel);

    long countByCityAndHealthLabel(String city, String healthLabel);

    long countByCity(String city);

    @Query("SELECT AVG(v.ndviSentinel) FROM VegetationPointEntity v WHERE v.city = :city")
    Double findCityMeanNdvi(@Param("city") String city);

    @Query("SELECT AVG(v.ndviLandsat) FROM VegetationPointEntity v WHERE v.city = :city")
    Double findCityMeanNdviLandsat(@Param("city") String city);

    @Query("SELECT v FROM VegetationPointEntity v WHERE v.city = :city ORDER BY v.ndviSentinel ASC")
    List<VegetationPointEntity> findByCityOrderByNdviAsc(@Param("city") String city);

    void deleteByCity(String city);

    // Legacy queries
    List<VegetationPointEntity> findByHealthLabel(String healthLabel);

    long countByHealthLabel(String healthLabel);

    @Query("SELECT AVG(v.ndviSentinel) FROM VegetationPointEntity v")
    Double findCityMeanNdvi();

    @Query("SELECT AVG(v.ndviLandsat) FROM VegetationPointEntity v")
    Double findCityMeanNdviLandsat();

    @Query("SELECT v FROM VegetationPointEntity v ORDER BY v.ndviSentinel ASC")
    List<VegetationPointEntity> findAllOrderByNdviAsc();
}
