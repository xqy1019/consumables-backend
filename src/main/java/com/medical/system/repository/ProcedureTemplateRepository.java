package com.medical.system.repository;

import com.medical.system.entity.ProcedureTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProcedureTemplateRepository extends JpaRepository<ProcedureTemplate, Long> {

    List<ProcedureTemplate> findByIsActiveTrueOrderByCreatedAtDesc();

    List<ProcedureTemplate> findByCategoryAndIsActiveTrue(String category);
}
