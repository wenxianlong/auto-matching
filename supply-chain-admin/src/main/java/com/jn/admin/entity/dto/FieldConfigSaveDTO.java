package com.jn.admin.entity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class FieldConfigSaveDTO {
    @NotBlank(message = "字段标识不能为空")
    private String fieldKey;

    @NotBlank(message = "字段名称不能为空")
    private String fieldName;

    @NotBlank(message = "字段类型不能为空")
    private String fieldType; // TEXT, NUMBER, DATE, SELECT

    private Integer maxLength;

    @NotNull(message = "是否必填不能为空")
    private Boolean isRequired;

    private Integer sortOrder;
    private String errorMsg;

    // 下拉框选项
    private List<Map<String, String>> dictOptions;
}

