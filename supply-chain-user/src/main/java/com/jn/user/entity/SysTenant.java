package com.jn.user.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("sys_tenant")
public class SysTenant {
    @TableId(type = IdType.AUTO) // 租户表数据量小，使用自增ID即可
    private Long id;
    private String name;
    private String code;
    private Integer status;
    private String contactUser;
    private String contactPhone;
    private LocalDateTime createTime;
}
