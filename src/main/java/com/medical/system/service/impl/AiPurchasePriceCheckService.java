package com.medical.system.service.impl;

import com.medical.system.dto.PriceCheckRequest;
import com.medical.system.dto.PriceCheckResult;
import com.medical.system.repository.PurchaseInquiryItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiPurchasePriceCheckService {

    private final PurchaseInquiryItemRepository inquiryItemRepository;

    public List<PriceCheckResult> checkPrices(List<PriceCheckRequest> items) {
        List<PriceCheckResult> results = new ArrayList<>();
        for (PriceCheckRequest item : items) {
            results.add(checkSinglePrice(item));
        }
        return results;
    }

    private PriceCheckResult checkSinglePrice(PriceCheckRequest item) {
        PriceCheckResult result = new PriceCheckResult();
        result.setMaterialId(item.getMaterialId());
        result.setMaterialName(item.getMaterialName());
        result.setCurrentPrice(item.getCurrentPrice());

        // 查历史价格（近20条）
        List<BigDecimal> historicalPrices = inquiryItemRepository
                .findHistoricalPrices(item.getMaterialId(), PageRequest.of(0, 20));

        if (historicalPrices == null || historicalPrices.isEmpty()) {
            result.setStatus("NORMAL");
            result.setDeviation(0.0);
            result.setReason("无历史价格数据可对比");
            return result;
        }

        // 计算历史均价
        BigDecimal sum = historicalPrices.stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal avg = sum.divide(BigDecimal.valueOf(historicalPrices.size()), 2, RoundingMode.HALF_UP);
        result.setAvgHistoricalPrice(avg);

        // 计算偏差
        if (avg.compareTo(BigDecimal.ZERO) > 0 && item.getCurrentPrice() != null) {
            double deviation = item.getCurrentPrice().subtract(avg)
                    .divide(avg, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .doubleValue();
            result.setDeviation(deviation);

            if (deviation > 20) {
                result.setStatus("ABNORMAL_HIGH");
                result.setReason(String.format("当前报价比历史均价高 %.1f%%，均价 %.2f", deviation, avg));
            } else if (deviation < -20) {
                result.setStatus("ABNORMAL_LOW");
                result.setReason(String.format("当前报价比历史均价低 %.1f%%，均价 %.2f", Math.abs(deviation), avg));
            } else {
                result.setStatus("NORMAL");
                result.setReason(String.format("价格在正常范围内（偏差 %.1f%%）", deviation));
            }
        } else {
            result.setStatus("NORMAL");
            result.setDeviation(0.0);
            result.setReason("价格正常");
        }

        return result;
    }
}
