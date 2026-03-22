package com.aetrix.repository;

import com.aetrix.entity.ActionItemEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ActionItemRepository extends JpaRepository<ActionItemEntity, Long> {

    // City-filtered queries
    List<ActionItemEntity> findByCity(String city);

    List<ActionItemEntity> findByCityAndPriorityOrderByPriorityScoreDesc(String city, String priority);

    List<ActionItemEntity> findByCityAndStatus(String city, String status);

    List<ActionItemEntity> findByCityOrderByPriorityScoreDesc(String city);

    long countByCityAndStatus(String city, String status);

    long countByCityAndPriority(String city, String priority);

    long countByCity(String city);

    Optional<ActionItemEntity> findByCityAndActionId(String city, String actionId);

    void deleteByCity(String city);

    // Legacy queries
    List<ActionItemEntity> findByPriorityOrderByPriorityScoreDesc(String priority);

    List<ActionItemEntity> findByStatus(String status);

    long countByStatus(String status);

    long countByPriority(String priority);

    Optional<ActionItemEntity> findByActionId(String actionId);

    List<ActionItemEntity> findAllByOrderByPriorityScoreDesc();
}
