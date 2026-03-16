package com.medical.system.repository;

import com.medical.system.entity.AutoReplenishmentLog;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AutoReplenishmentLogRepository extends JpaRepository<AutoReplenishmentLog, Long> {

    List<AutoReplenishmentLog> findByDeptIdOrderByCreatedAtDesc(Long deptId);
}
