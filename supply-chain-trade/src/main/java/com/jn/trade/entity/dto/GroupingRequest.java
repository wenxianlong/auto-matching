package com.jn.trade.entity.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class GroupingRequest {
    // 原有基础参数
    private Integer targetDiameter;
    private BigDecimal minTargetHeight;
    private BigDecimal maxTargetHeight;
    private BigDecimal minTargetVoltage;
    private BigDecimal maxTargetVoltage;
    private String mode;

    // 新增：测试参数范围约束 (允许为空，为空则不参与筛选)
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

