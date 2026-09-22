package com.jn.trade.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.math.BigDecimal;

@Data
@TableName("mfg_resistor")
public class MfgResistor {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private String model;
    private String code;          // 新增
    private String batchNo;       // 新增
    private Integer diameter;
    private BigDecimal height;
    private BigDecimal voltage;
    private Integer stock;        // 注意：升级后建议 stock=1，每片独立记录

    // 新增测试字段
    private BigDecimal highSquareWave;
    private BigDecimal largeCurrent;
    private BigDecimal leakageCurrent;
    private BigDecimal residualVoltage;
    private BigDecimal heavyTransfer;
    private BigDecimal totalCurrent;
}

