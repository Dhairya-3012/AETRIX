package com.aetrix.repository;

import com.aetrix.entity.ActionItemEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ActionItemRepository extends JpaRepository<ActionItemEntity, Long> {

    List<ActionItemEntity> findByPriorityOrderByPriorityScoreDesc(String priority);

    List<ActionItemEntity> findByStatus(String status);

    long countByStatus(String status);

    long countByPriority(String priority);

    Optional<ActionItemEntity> findByActionId(String actionId);

    List<ActionItemEntity> findAllByOrderByPriorityScoreDesc();
}
