package com.medical.system.repository;

import com.medical.system.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findByUsernameAndStatus(String username, Integer status);
    boolean existsByUsername(String username);

    @Query("SELECT u FROM User u WHERE " +
           "(:keyword IS NULL OR u.username LIKE %:keyword% OR u.realName LIKE %:keyword%) AND " +
           "(:deptId IS NULL OR u.deptId = :deptId) AND " +
           "(:status IS NULL OR u.status = :status)")
    Page<User> findByConditions(@Param("keyword") String keyword,
                                @Param("deptId") Long deptId,
                                @Param("status") Integer status,
                                Pageable pageable);
}
