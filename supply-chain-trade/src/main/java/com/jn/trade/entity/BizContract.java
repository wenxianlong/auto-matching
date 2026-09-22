package com.jn.trade.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@Data
@TableName(value = "biz_contract", autoResultMap = true) // 【关键】：必须开启 autoResultMap
public class BizContract {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long tenantId;
    private String contractNo;
    private Long categoryId;
    private BigDecimal amount;
    private String status;

    // 【关键】：使用 JacksonTypeHandler 自动将 JSON 映射为 Map
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> extraData;

    private Long createBy;
    private LocalDateTime createTime;
}
