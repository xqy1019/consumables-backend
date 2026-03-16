package com.medical.system.repository;

import com.medical.system.entity.AnomalyWorkOrderComment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AnomalyWorkOrderCommentRepository extends JpaRepository<AnomalyWorkOrderComment, Long> {
    List<AnomalyWorkOrderComment> findByWorkOrderIdOrderByCreatedAtAsc(Long workOrderId);
}
