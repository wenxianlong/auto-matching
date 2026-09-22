package com.jn.admin.entity.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CategoryCreateDTO {
    @NotBlank(message = "品类名称不能为空")
    private String name;

    @NotBlank(message = "品类编码不能为空")
    private String code;
}

