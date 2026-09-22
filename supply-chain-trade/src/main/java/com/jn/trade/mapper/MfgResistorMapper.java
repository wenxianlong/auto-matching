package com.jn.trade.mapper;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jn.trade.entity.MfgResistor;

//@DS("sqlserver") 如果使用sqlserver数据库，添加此注解
public interface MfgResistorMapper extends BaseMapper<MfgResistor> {

//    如果 SQL Server 的表不需要多租户，可以在 `TenantLineHandler` 的 `ignoreTable` 中忽略，或者给特定的 Mapper 加 `@InterceptorIgnore(tenantLine = "true")`。

}
