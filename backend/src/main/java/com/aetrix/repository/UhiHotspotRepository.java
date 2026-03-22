package com.aetrix.repository;

import com.aetrix.entity.UhiHotspotEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UhiHotspotRepository extends JpaRepository<UhiHotspotEntity, Long> {

    // City-filtered queries
    List<UhiHotspotEntity> findByCity(String city);

    List<UhiHotspotEntity> findByCityAndSeverity(String city, String severity);

    List<UhiHotspotEntity> findByCityOrderByAvgLstCelsiusDesc(String city);

    long countByCity(String city);

    void deleteByCity(String city);

    // Legacy queries
    List<UhiHotspotEntity> findBySeverity(String severity);

    List<UhiHotspotEntity> findAllByOrderByAvgLstCelsiusDesc();
}
