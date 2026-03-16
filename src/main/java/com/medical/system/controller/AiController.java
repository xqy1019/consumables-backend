package com.medical.system.controller;

import com.medical.system.common.PageResult;
import com.medical.system.common.Result;
import com.medical.system.dto.*;
import com.medical.system.entity.AiPredictionResult;
import com.medical.system.repository.AiPredictionResultRepository;
import com.medical.system.service.impl.AiAnomalyService;
import com.medical.system.service.impl.AiChatService;
import com.medical.system.service.impl.AiDashboardService;
import com.medical.system.service.impl.AiExpiryDisposalService;
import com.medical.system.service.impl.AiInsightService;
import com.medical.system.service.impl.AiPredictionServiceImpl;
import com.medical.system.service.impl.AiPredictionServiceImpl.*;
import com.medical.system.service.impl.AiPurchasePriceCheckService;
import com.medical.system.service.impl.AiRequisitionRecommendService;
import com.medical.system.service.impl.AiRequisitionReviewService;
import com.medical.system.service.impl.AiSupplierRecommendService;
import com.medical.system.service.impl.ClaudeService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.YearMonth;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
public class AiController {

    private final AiPredictionServiceImpl aiService;
    private final AiInsightService aiInsightService;
    private final AiDashboardService aiDashboardService;
    private final AiChatService aiChatService;
    private final ClaudeService claudeService;
    private final AiRequisitionRecommendService aiRequisitionRecommendService;
    private final AiRequisitionReviewService aiRequisitionReviewService;
    private final AiExpiryDisposalService aiExpiryDisposalService;
    private final AiAnomalyService aiAnomalyService;
    private final AiPurchasePriceCheckService aiPurchasePriceCheckService;
    private final AiSupplierRecommendService aiSupplierRecommendService;
    private final AiPredictionResultRepository predictionResultRepository;

    @GetMapping("/predictions")
    @PreAuthorize("hasAuthority('menu:ai')")
    public Result<PageResult<PredictionVO>> getPredictions(
            @RequestParam(required = false) String month,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return Result.success(aiService.getPredictions(month,
                PageRequest.of(page - 1, size, Sort.by("id").descending())));
    }

    @PostMapping("/predict")
    @PreAuthorize("hasAuthority('menu:ai')")
    public Result<String> triggerPredict() {
        int count = aiService.triggerPredict();
        return Result.success("已为 " + count + " 种耗材生成预测数据");
    }

    @GetMapping("/safety-stock")
    @PreAuthorize("hasAuthority('menu:inventory')")
    public Result<List<SafetyStockVO>> getSafetyStock() {
        return Result.success(aiService.getSafetyStock());
    }

    @GetMapping("/warnings")
    @PreAuthorize("hasAuthority('menu:inventory')")
    public Result<List<WarningVO>> getWarnings() {
        return Result.success(aiService.getWarnings());
    }

    @GetMapping("/shortage-warnings")
    @PreAuthorize("hasAuthority('menu:inventory')")
    public Result<List<WarningVO>> getShortageWarnings() {
        return Result.success(aiService.getShortageWarnings());
    }

    @GetMapping("/insight")
    @PreAuthorize("hasAuthority('menu:ai')")
    public Result<String> getInsight() {
        return Result.success(aiInsightService.getInsight());
    }

    @GetMapping("/dashboard-analysis")
    @PreAuthorize("hasAuthority('menu:ai')")
    public Result<AiDashboardService.AiDashboardVO> getDashboardAnalysis() {
        return Result.success(aiDashboardService.getDashboardAnalysis());
    }

    @PostMapping("/chat")
    @PreAuthorize("hasAuthority('menu:ai')")
    public Result<AiChatService.ChatResponseVO> chat(@RequestBody ChatRequest req) {
        String msg = req.getMessage();
        if (msg == null || msg.isBlank()) {
            return Result.success(new AiChatService.ChatResponseVO(
                    "您好！请输入您想了解的内容，例如：当前有哪些预警？", null));
        }
        return Result.success(aiChatService.chat(msg.trim()));
    }

    @PostMapping("/analyze-report")
    @PreAuthorize("hasAuthority('menu:ai')")
    public Result<String> analyzeReport(@RequestBody ReportAnalysisRequest req) {
        if (req.getData() == null || req.getData().isBlank()) {
            return Result.success(null);
        }
        String systemPrompt = """
                你是医疗耗材管理系统的数据分析专家。请对用户提供的报表数据进行专业解读，
                给出关键发现、异常分析和改善建议。语言简洁、客观，重点突出，
                控制在200字以内，使用分点格式。
                """;
        String userMsg = "报表类型：" + req.getReportType() + "\n\n数据摘要：\n" + req.getData();
        String result = claudeService.chatWithSystem(systemPrompt, userMsg, 500);
        return Result.success(result);
    }

    @GetMapping("/recommend-requisition")
    @PreAuthorize("hasAuthority('menu:requisition')")
    public Result<List<RequisitionRecommendationVO>> getRequisitionRecommendations(
            @RequestParam Long deptId) {
        return Result.success(aiRequisitionRecommendService.recommend(deptId));
    }

    @GetMapping("/requisition-review/{id}")
    @PreAuthorize("hasAuthority('menu:requisition')")
    public Result<List<RequisitionReviewItemVO>> reviewRequisition(@PathVariable Long id) {
        return Result.success(aiRequisitionReviewService.review(id));
    }

    @GetMapping("/expiry-disposal")
    @PreAuthorize("hasAuthority('menu:ai')")
    public Result<List<ExpiryDisposalVO>> getExpiryDisposal() {
        return Result.success(aiExpiryDisposalService.getDisposalAdvice());
    }

    @GetMapping("/anomaly-detection")
    @PreAuthorize("hasAuthority('menu:ai')")
    public Result<List<AnomalyVO>> getAnomalyDetection() {
        return Result.success(aiAnomalyService.detectAnomalies());
    }

    @PostMapping("/check-purchase-price")
    @PreAuthorize("hasAuthority('menu:purchase')")
    public Result<List<PriceCheckResult>> checkPurchasePrice(@RequestBody List<PriceCheckRequest> items) {
        return Result.success(aiPurchasePriceCheckService.checkPrices(items));
    }

    @GetMapping("/recommend-supplier")
    @PreAuthorize("hasAuthority('menu:purchase')")
    public Result<List<Map<String, Object>>> recommendSupplier(
            @RequestParam Long materialId,
            @RequestParam(defaultValue = "1") Integer quantity) {
        return Result.success(aiSupplierRecommendService.recommend(materialId, quantity));
    }

    @GetMapping("/prediction-accuracy")
    @PreAuthorize("hasAuthority('menu:ai')")
    public Result<Map<String, Object>> getPredictionAccuracy(
            @RequestParam(required = false) String month) {
        if (month == null) {
            month = YearMonth.now().minusMonths(1).toString();
        }
        List<AiPredictionResult> predictions = predictionResultRepository.findByPredictionMonth(month);
        Map<String, Object> result = new HashMap<>();
        result.put("month", month);
        if (predictions != null && !predictions.isEmpty()) {
            double avgAccuracy = predictions.stream()
                    .filter(p -> p.getAccuracyRate() != null)
                    .mapToDouble(p -> p.getAccuracyRate().doubleValue())
                    .average().orElse(0.0);
            result.put("avgAccuracyRate", avgAccuracy);
            result.put("evaluatedCount", predictions.stream().filter(p -> p.getEvaluatedAt() != null).count());
            result.put("predictions", predictions);
        } else {
            result.put("avgAccuracyRate", 0.0);
            result.put("evaluatedCount", 0);
            result.put("predictions", List.of());
        }
        return Result.success(result);
    }

    @Data
    static class ChatRequest {
        private String message;
    }

    @Data
    static class ReportAnalysisRequest {
        private String reportType;
        private String data;
    }
}
