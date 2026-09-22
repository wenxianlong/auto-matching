package com.jn.trade.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("biz_category_field")
public class BizCategoryField {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long categoryId;
    private String fieldKey;
    private String fieldLabel;
    private String fieldType;
    private Integer isRequired;
    private String dictType;
    private Integer sortOrder;
}
