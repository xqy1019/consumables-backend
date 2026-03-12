package com.medical.system.service.impl;

import com.medical.system.entity.OperationLog;
import com.medical.system.repository.OperationLogRepository;
import com.medical.system.service.OperationLogService;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class OperationLogServiceImpl implements OperationLogService {

    private final OperationLogRepository operationLogRepository;

    @Async
    @Override
    public void saveLog(OperationLog operationLog) {
        try {
            operationLogRepository.save(operationLog);
        } catch (Exception e) {
            log.warn("保存操作日志失败: {}", e.getMessage());
        }
    }

    @Override
    public Page<OperationLog> page(String username, String module, Integer status,
                                   LocalDateTime startTime, LocalDateTime endTime,
                                   int pageNum, int pageSize) {
        Specification<OperationLog> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (StringUtils.hasText(username)) {
                predicates.add(cb.like(root.get("username"), "%" + username + "%"));
            }
            if (StringUtils.hasText(module)) {
                predicates.add(cb.equal(root.get("module"), module));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (startTime != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("operateTime"), startTime));
            }
            if (endTime != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("operateTime"), endTime));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        PageRequest pageable = PageRequest.of(pageNum - 1, pageSize, Sort.by(Sort.Direction.DESC, "operateTime"));
        return operationLogRepository.findAll(spec, pageable);
    }
}
