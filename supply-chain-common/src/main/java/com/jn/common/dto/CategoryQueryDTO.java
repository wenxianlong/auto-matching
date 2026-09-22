package com.jn.common.dto;

import lombok.Data;

@Data
public class CategoryQueryDTO {
    /**
     * 品类名称 (模糊查询)
     */
    private String name;

    /**
     * 品类编码 (精确查询)
     */
    private String code;

    /**
     * 当前页码 (默认 1)
     */
    private Integer pageNum = 1;

    /**
     * 每页条数 (默认 10)
     */
    private Integer pageSize = 10;
}

