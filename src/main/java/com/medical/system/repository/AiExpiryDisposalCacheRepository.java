package com.medical.system.repository;

import com.medical.system.entity.AiExpiryDisposalCache;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface AiExpiryDisposalCacheRepository extends JpaRepository<AiExpiryDisposalCache, Long> {
    Optional<AiExpiryDisposalCache> findByInventoryId(Long inventoryId);
}
