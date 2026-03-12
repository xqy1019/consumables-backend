package com.medical.system.repository;

import com.medical.system.entity.RecallDisposal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RecallDisposalRepository extends JpaRepository<RecallDisposal, Long> {
    List<RecallDisposal> findByRecallId(Long recallId);
}
