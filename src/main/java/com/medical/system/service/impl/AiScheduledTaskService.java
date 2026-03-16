package com.medical.system.service.impl;

import com.medical.system.entity.AiPredictionResult;
import com.medical.system.repository.AiPredictionResultRepository;
import com.medical.system.repository.InventoryTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiScheduledTaskService {

    private final AiExpiryDisposalService aiExpiryDisposalService;
    private final AiPredictionResultRepository predictionResultRepository;
    private final InventoryTransactionRepository inventoryTransactionRepository;
    private final SmallConsumableService smallConsumableService;
    private final AnomalyWorkOrderService anomalyWorkOrderService;

    /**
     * 每天早上8点刷新临期处置建议缓存
     */
    @Scheduled(cron = "0 0 8 * * ?")
    @Transactional
    public void refreshExpiryDisposalCache() {
        log.info("开始刷新临期处置建议缓存...");
        try {
            // 调用处置建议服务，内部会清除旧缓存并写入新数据
            aiExpiryDisposalService.getDisposalAdvice();
            log.info("临期处置建议缓存刷新完成");
        } catch (Exception e) {
            log.error("刷新临期处置建议缓存失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 每月1日凌晨2点评估上月预测准确率
     */
    @Scheduled(cron = "0 0 2 1 * ?")
    @Transactional
    public void evaluatePredictionAccuracy() {
        log.info("开始评估预测准确率...");
        try {
            YearMonth lastMonth = YearMonth.now().minusMonths(1);
            String monthStr = lastMonth.toString(); // "yyyy-MM"

            List<AiPredictionResult> predictions = predictionResultRepository.findByPredictionMonth(monthStr);
            if (predictions == null || predictions.isEmpty()) {
                log.info("上月({})无预测记录", monthStr);
                return;
            }

            LocalDateTime start = lastMonth.atDay(1).atStartOfDay();
            LocalDateTime end = start.plusMonths(1);
            int processed = 0;

            for (AiPredictionResult p : predictions) {
                Integer actual = inventoryTransactionRepository.sumActualOutbound(
                        p.getMaterialId(), start, end);
                if (actual == null) actual = 0;

                int pred = p.getPredictedQuantity() != null ? p.getPredictedQuantity() : 0;
                BigDecimal accuracy;
                if (pred > 0 || actual > 0) {
                    int maxVal = Math.max(actual, pred);
                    int minVal = Math.min(actual, pred);
                    accuracy = maxVal > 0
                            ? BigDecimal.valueOf(minVal * 100.0 / maxVal)
                            : BigDecimal.ZERO;
                } else {
                    accuracy = BigDecimal.valueOf(100); // 都是0，准确率100%
                }

                p.setActualQuantity(actual);
                p.setAccuracyRate(accuracy);
                p.setEvaluatedAt(LocalDateTime.now());
                predictionResultRepository.save(p);
                processed++;
            }

            log.info("预测准确率评估完成，共处理 {} 条记录", processed);
        } catch (Exception e) {
            log.error("评估预测准确率失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 每2小时检查工单 SLA，超期自动升级优先级
     */
    @Scheduled(cron = "0 0 */2 * * ?")
    @Transactional
    public void checkWorkOrderSLA() {
        log.info("开始检查工单 SLA...");
        try {
            int escalated = anomalyWorkOrderService.checkAndEscalateSLA();
            log.info("工单 SLA 检查完成，升级 {} 个工单", escalated);
        } catch (Exception e) {
            log.error("工单 SLA 检查失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 每天早上9点检查当月消耗异常并自动生成 DANGER 工单
     */
    @Scheduled(cron = "0 0 9 * * ?")
    @Transactional
    public void autoCreateAnomalyWorkOrders() {
        log.info("开始检查当月消耗异常并自动生成工单...");
        try {
            // 系统用户 ID=1
            int created = smallConsumableService.autoCreateWorkOrdersForAnomalies(null, 1L);
            log.info("异常工单自动生成完成，新创建 {} 个工单", created);
        } catch (Exception e) {
            log.error("异常工单自动生成失败: {}", e.getMessage(), e);
        }
    }
}
