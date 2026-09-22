package com.jn.admin.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
// 必须开启 autoResultMap=true，否则 JSON 类型处理器无法生效
@TableName(value = "category_field_config", autoResultMap = true)
public class CategoryFieldConfig {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long tenantId;
    private Long categoryId;
    private String fieldKey;
    private String fieldName;
    private String fieldType;
    private Integer maxLength;
    private Boolean isRequired;
    private Integer sortOrder;
    private String errorMsg;

    // 使用 JacksonTypeHandler 映射 JSON 字段
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<Map<String, String>> dictOptions;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}

