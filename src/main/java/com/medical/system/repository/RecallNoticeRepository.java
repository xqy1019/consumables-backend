package com.medical.system.repository;

import com.medical.system.entity.RecallNotice;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RecallNoticeRepository extends JpaRepository<RecallNotice, Long> {

    List<RecallNotice> findByStatus(String status);

    @Query("SELECT r FROM RecallNotice r WHERE " +
           "(:status IS NULL OR r.status = :status) AND " +
           "(:keyword IS NULL OR r.title LIKE %:keyword% OR r.recallNo LIKE %:keyword%)")
    Page<RecallNotice> findByConditions(@Param("status") String status,
                                        @Param("keyword") String keyword,
                                        Pageable pageable);
}
