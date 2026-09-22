package com.jn.trade.entity.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class PieceUsage {
    private Long resistorId;
    private String model;
    private String code;           // 新增
    private String batchNo;        // 新增
    private BigDecimal height;
    private BigDecimal voltage;
    private Integer usedCount;     // 升级后固定为 1
    private Integer currentStock;  // 升级后固定为 1

    // 新增测试字段展示
    private BigDecimal highSquareWave;
    private BigDecimal largeCurrent;
    private BigDecimal leakageCurrent;
    private BigDecimal residualVoltage;
    private BigDecimal heavyTransfer;
    private BigDecimal totalCurrent;
}
