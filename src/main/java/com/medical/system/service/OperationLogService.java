package com.medical.system.service;

import com.medical.system.entity.OperationLog;
import org.springframework.data.domain.Page;
import java.time.LocalDateTime;

public interface OperationLogService {
    void saveLog(OperationLog log);
    Page<OperationLog> page(String username, String module, Integer status,
                            LocalDateTime startTime, LocalDateTime endTime,
                            int pageNum, int pageSize);
}
