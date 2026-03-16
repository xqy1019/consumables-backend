package com.medical.system.repository;

import com.medical.system.entity.ProcedureTemplateItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProcedureTemplateItemRepository extends JpaRepository<ProcedureTemplateItem, Long> {

    List<ProcedureTemplateItem> findByTemplateId(Long templateId);

    void deleteByTemplateId(Long templateId);
}
