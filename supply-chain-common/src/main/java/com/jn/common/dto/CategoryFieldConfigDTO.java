package com.jn.common.dto;

import lombok.Data;

@Data
public class CategoryFieldConfigDTO {
    private Long id;
    private Long categoryId;
    private String fieldKey;
    private String fieldName;
    private String fieldType;
    private Integer maxLength;
    private Boolean isRequired;
    private Integer sortOrder;
    private String errorMsg;
}

