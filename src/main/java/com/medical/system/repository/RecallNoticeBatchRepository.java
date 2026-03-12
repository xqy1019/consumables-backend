package com.medical.system.repository;

import com.medical.system.entity.RecallNoticeBatch;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RecallNoticeBatchRepository extends JpaRepository<RecallNoticeBatch, Long> {
    List<RecallNoticeBatch> findByRecallId(Long recallId);
}
