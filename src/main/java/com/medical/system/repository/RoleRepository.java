package com.medical.system.repository;

import com.medical.system.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByRoleCode(String roleCode);
    boolean existsByRoleCode(String roleCode);
    List<Role> findByStatus(Integer status);
}
