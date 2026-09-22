package com.jn.trade.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.jn.trade.entity.MfgResistor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class GroupingAlgorithmService {

    // 500万次搜索上限，配合强力剪枝，现代CPU只需几十毫秒
    private static final int MAX_SEARCH_COUNT = 5000000;
    private int searchCount = 0;

    // 放大倍数：10000倍，支持4位小数，彻底杜绝 longValue() 截断导致的精度丢失
    private static final long MULTIPLIER = 10000L;

    @Data
    public static class GroupingRequest {
        private Integer targetDiameter;
        private BigDecimal minTargetHeight;
        private BigDecimal maxTargetHeight;
        private BigDecimal minTargetVoltage;
        private BigDecimal maxTargetVoltage;
        private String mode;

        private BigDecimal minHighSquareWave;
        private BigDecimal maxHighSquareWave;
        private BigDecimal minLargeCurrent;
        private BigDecimal maxLargeCurrent;
        private BigDecimal minLeakageCurrent;
        private BigDecimal maxLeakageCurrent;
        private BigDecimal minResidualVoltage;
        private BigDecimal maxResidualVoltage;
        private BigDecimal minHeavyTransfer;
        private BigDecimal maxHeavyTransfer;
        private BigDecimal minTotalCurrent;
        private BigDecimal maxTotalCurrent;
    }

    @Data
    public static class PieceUsage {
        private Long resistorId;
        private String model;
        private String code;
        private String batchNo;
        private BigDecimal height;
        private BigDecimal voltage;
        private Integer usedCount;     // 该型号实际使用的片数
        private Integer currentStock;  // 当前总库存
        private BigDecimal highSquareWave;
        private BigDecimal largeCurrent;
        private BigDecimal leakageCurrent;
        private BigDecimal residualVoltage;
        private BigDecimal heavyTransfer;
        private BigDecimal totalCurrent;
    }

    @Data
    public static class GroupingResult {
        private BigDecimal totalHeight;
        private BigDecimal totalVoltage;
        private Integer totalPieces;
        private Integer maxProducibleGroups;
        private List<PieceUsage> details;
    }

    public LambdaQueryWrapper<MfgResistor> buildFilterWrapper(GroupingRequest req) {
        LambdaQueryWrapper<MfgResistor> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MfgResistor::getDiameter, req.getTargetDiameter())
                .gt(MfgResistor::getStock, 0);

        if (req.getMinHighSquareWave() != null) wrapper.ge(MfgResistor::getHighSquareWave, req.getMinHighSquareWave());
        if (req.getMaxHighSquareWave() != null) wrapper.le(MfgResistor::getHighSquareWave, req.getMaxHighSquareWave());
        if (req.getMinLargeCurrent() != null) wrapper.ge(MfgResistor::getLargeCurrent, req.getMinLargeCurrent());
        if (req.getMaxLargeCurrent() != null) wrapper.le(MfgResistor::getLargeCurrent, req.getMaxLargeCurrent());
        if (req.getMinLeakageCurrent() != null) wrapper.ge(MfgResistor::getLeakageCurrent, req.getMinLeakageCurrent());
        if (req.getMaxLeakageCurrent() != null) wrapper.le(MfgResistor::getLeakageCurrent, req.getMaxLeakageCurrent());
        if (req.getMinResidualVoltage() != null) wrapper.ge(MfgResistor::getResidualVoltage, req.getMinResidualVoltage());
        if (req.getMaxResidualVoltage() != null) wrapper.le(MfgResistor::getResidualVoltage, req.getMaxResidualVoltage());
        if (req.getMinHeavyTransfer() != null) wrapper.ge(MfgResistor::getHeavyTransfer, req.getMinHeavyTransfer());
        if (req.getMaxHeavyTransfer() != null) wrapper.le(MfgResistor::getHeavyTransfer, req.getMaxHeavyTransfer());
        if (req.getMinTotalCurrent() != null) wrapper.ge(MfgResistor::getTotalCurrent, req.getMinTotalCurrent());
        if (req.getMaxTotalCurrent() != null) wrapper.le(MfgResistor::getTotalCurrent, req.getMaxTotalCurrent());

        return wrapper;
    }

    public List<GroupingResult> calculate(List<MfgResistor> candidates, GroupingRequest req) {
        searchCount = 0;
        if (candidates == null || candidates.isEmpty()) return Collections.emptyList();

        sortResistors(candidates, req);

        int n = candidates.size();
        long[] hArr = new long[n];
        long[] vArr = new long[n];
        int[] stockArr = new int[n];

        // 使用 HALF_UP 四舍五入，防止 longValue() 直接截断小数导致误差
        long minH = req.getMinTargetHeight().multiply(BigDecimal.valueOf(MULTIPLIER)).setScale(0, RoundingMode.HALF_UP).longValue();
        long maxH = req.getMaxTargetHeight().multiply(BigDecimal.valueOf(MULTIPLIER)).setScale(0, RoundingMode.HALF_UP).longValue();
        long minV = req.getMinTargetVoltage().multiply(BigDecimal.valueOf(MULTIPLIER)).setScale(0, RoundingMode.HALF_UP).longValue();
        long maxV = req.getMaxTargetVoltage().multiply(BigDecimal.valueOf(MULTIPLIER)).setScale(0, RoundingMode.HALF_UP).longValue();

        for (int i = 0; i < n; i++) {
            MfgResistor r = candidates.get(i);
            hArr[i] = r.getHeight().multiply(BigDecimal.valueOf(MULTIPLIER)).setScale(0, RoundingMode.HALF_UP).longValue();
            vArr[i] = r.getVoltage().multiply(BigDecimal.valueOf(MULTIPLIER)).setScale(0, RoundingMode.HALF_UP).longValue();
            stockArr[i] = r.getStock();
        }

        // 【核心优化 1】：计算多重背包的后缀和潜力 (假设把后面所有库存都选满)
        long[] suffixH = new long[n + 1];
        long[] suffixV = new long[n + 1];
        for (int i = n - 1; i >= 0; i--) {
            suffixH[i] = suffixH[i + 1] + (hArr[i] * stockArr[i]);
            suffixV[i] = suffixV[i + 1] + (vArr[i] * stockArr[i]);
        }

        List<GroupingResult> results = new ArrayList<>();
        int[] selectedCounts = new int[n]; // 记录每个型号选了多少片

        dfs(candidates, hArr, vArr, stockArr, suffixH, suffixV, 0, 0, 0,
                minH, maxH, minV, maxV, selectedCounts, results);

        return results.stream()
                .sorted((r1, r2) -> {
                    int cmp = Integer.compare(r2.getMaxProducibleGroups(), r1.getMaxProducibleGroups());
                    if (cmp != 0) return cmp;
                    return Integer.compare(r1.getTotalPieces(), r2.getTotalPieces());
                })
                .limit(3)
                .collect(Collectors.toList());
    }

    /**
     * 多重背包 DFS + 双重剪枝
     */
    private void dfs(List<MfgResistor> resistors, long[] hArr, long[] vArr, int[] stockArr,
                     long[] suffixH, long[] suffixV, int index,
                     long curH, long curV, long minH, long maxH, long minV, long maxV,
                     int[] selectedCounts, List<GroupingResult> results) {

        if (searchCount++ > MAX_SEARCH_COUNT) return;

        // 1. 满足目标区间，记录方案
        if (curH >= minH && curH <= maxH && curV >= minV && curV <= maxV) {
            // 确保至少选了1片
            boolean hasSelected = false;
            for (int count : selectedCounts) {
                if (count > 0) { hasSelected = true; break; }
            }
            if (hasSelected) {
                results.add(buildResult(resistors, selectedCounts, curH, curV));
                if (results.size() >= 50) return;
            }
        }

        // 2. 已经遍历完所有物品
        if (index == resistors.size()) return;

        // 【核心优化 2】：上限剪枝 (当前值已经超过最大限制)
        if (curH > maxH || curV > maxV) return;

        // 【核心优化 3】：潜力剪枝 (当前值 + 剩下所有物品的最大库存总和 < 最小限制)
        if (curH + suffixH[index] < minH || curV + suffixV[index] < minV) return;

        // 3. 多重背包选择：尝试选择当前物品 k 次 (0 到 stock)
        int maxK = stockArr[index];
        for (int k = 0; k <= maxK; k++) {
            long nextH = curH + (k * hArr[index]);
            long nextV = curV + (k * vArr[index]);

            // 优化：如果选 k 次已经超限，选 k+1 次肯定也超限，直接 break 终止当前物品的循环
            if (nextH > maxH || nextV > maxV) break;

            selectedCounts[index] = k;
            dfs(resistors, hArr, vArr, stockArr, suffixH, suffixV, index + 1,
                    nextH, nextV, minH, maxH, minV, maxV, selectedCounts, results);
        }

        // 回溯清理
        selectedCounts[index] = 0;
    }

    private GroupingResult buildResult(List<MfgResistor> resistors, int[] selectedCounts,
                                       long totalH, long totalV) {
        GroupingResult result = new GroupingResult();
        result.setTotalHeight(BigDecimal.valueOf(totalH).divide(BigDecimal.valueOf(MULTIPLIER), 4, RoundingMode.HALF_UP));
        result.setTotalVoltage(BigDecimal.valueOf(totalV).divide(BigDecimal.valueOf(MULTIPLIER), 4, RoundingMode.HALF_UP));

        int totalPieces = 0;
        int maxGroups = Integer.MAX_VALUE;
        List<PieceUsage> details = new ArrayList<>();

        for (int i = 0; i < selectedCounts.length; i++) {
            int k = selectedCounts[i];
            if (k > 0) {
                MfgResistor r = resistors.get(i);
                PieceUsage u = new PieceUsage();
                u.setResistorId(r.getId());
                u.setModel(r.getModel());
                u.setCode(r.getCode());
                u.setBatchNo(r.getBatchNo());
                u.setHeight(r.getHeight());
                u.setVoltage(r.getVoltage());
                u.setUsedCount(k);
                u.setCurrentStock(r.getStock());
                u.setHighSquareWave(r.getHighSquareWave());
                u.setLargeCurrent(r.getLargeCurrent());
                u.setLeakageCurrent(r.getLeakageCurrent());
                u.setResidualVoltage(r.getResidualVoltage());
                u.setHeavyTransfer(r.getHeavyTransfer());
                u.setTotalCurrent(r.getTotalCurrent());
                details.add(u);

                totalPieces += k;
                int canProduce = r.getStock() / k;
                if (canProduce < maxGroups) maxGroups = canProduce;
            }
        }

        result.setTotalPieces(totalPieces);
        result.setDetails(details);
        result.setMaxProducibleGroups(maxGroups == Integer.MAX_VALUE ? 0 : maxGroups);
        return result;
    }

    private void sortResistors(List<MfgResistor> list, GroupingRequest req) {
        switch (req.getMode() != null ? req.getMode() : "") {
            case "LEAST_STOCK": list.sort(Comparator.comparingInt(MfgResistor::getStock)); break;
            case "MOST_STOCK": list.sort(Comparator.comparingInt(MfgResistor::getStock).reversed()); break;
            case "FIT_VOLTAGE": list.sort(Comparator.comparing(MfgResistor::getVoltage).reversed()); break;
            case "FIT_HEIGHT": list.sort(Comparator.comparing(MfgResistor::getHeight).reversed()); break;
            default: break;
        }
    }
}


