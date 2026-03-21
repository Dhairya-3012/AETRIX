package com.aetrix.repository;

import com.aetrix.entity.UhiHotspotEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UhiHotspotRepository extends JpaRepository<UhiHotspotEntity, Long> {

    List<UhiHotspotEntity> findBySeverity(String severity);

    List<UhiHotspotEntity> findAllByOrderByAvgLstCelsiusDesc();
}
