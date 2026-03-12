package com.medical.system.repository;

import com.medical.system.entity.SurgeryMaterialBinding;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SurgeryMaterialBindingRepository extends JpaRepository<SurgeryMaterialBinding, Long> {
    List<SurgeryMaterialBinding> findBySurgeryId(Long surgeryId);
    List<SurgeryMaterialBinding> findByMaterialId(Long materialId);
    List<SurgeryMaterialBinding> findByUdiId(Long udiId);
}
