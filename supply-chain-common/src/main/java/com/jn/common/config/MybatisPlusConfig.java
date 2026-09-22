package com.jn.common.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.handler.DataPermissionHandler;
import com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler;
import com.baomidou.mybatisplus.extension.plugins.inner.DataPermissionInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import com.jn.common.context.UserContext;
import jakarta.servlet.http.HttpServletRequest;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.expression.NullValue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Arrays;
import java.util.List;

@Configuration
public class MybatisPlusConfig {

    // 不需要多租户隔离的全局表
    private static final List<String> IGNORE_TENANT_TABLES = Arrays.asList(
            "sys_tenant", "sys_menu", "sys_user_role", "sys_role_menu",
            "sys_dict_type", "sys_dict_data",// 必须保留在这里，由 Service 层手动控制 OR 逻辑
            "biz_category_field",
            "biz_tenant_category"
            // 【新增】：将 SQL Server 中不需要多租户隔离的表名加在这里
//            "erp_inventory", "erp_material"
    );

    /**
     * 【核心工具方法】：判断当前请求是否是 C端(服务端) 请求
     */
    private boolean isAppRequest() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                String uri = request.getRequestURI();
                // 如果 URI 以 /app/ 开头，说明是 C端请求
                return uri != null && uri.startsWith("/app/");
            }
        } catch (Exception e) {
            // 忽略非 Web 上下文（如定时任务、MQ 消费者）中的异常
        }
        return false;
    }

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();

        // 1. 多租户插件
        interceptor.addInnerInterceptor(new TenantLineInnerInterceptor(new TenantLineHandler() {
            @Override
            public Expression getTenantId() {
                // 优先使用超管指定的目标租户
                Long targetId = UserContext.getTargetTenantId();
                if (targetId != null) return new LongValue(targetId);

                // 其次使用当前登录用户的租户
                Long tenantId = UserContext.getTenantId();
                return tenantId != null ? new LongValue(tenantId) : new NullValue();
            }

            @Override
            public String getTenantIdColumn() {
                return "tenant_id";
            }

            @Override
            public boolean ignoreTable(String tableName) {
                // 【新增】：获取当前请求的 URI，如果是 /app/ 开头的 C端请求，直接忽略多租户拦截！
                ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
                if (attributes != null) {
                    String uri = attributes.getRequest().getRequestURI();
                    if (uri.startsWith("/app/")) {
                        return true; // C端请求不进行多租户 SQL 改写
                    }
                }

                // 1. 全局表直接忽略
                if (IGNORE_TENANT_TABLES.contains(tableName)) return true;

                // 2. 如果超管指定了目标租户，强制不忽略（必须拼接目标租户条件）
                if (UserContext.getTargetTenantId() != null) return false;

                Long currentTenantId = UserContext.getTenantId();
                // 3. 如果上下文中没有 tenantId（如登录阶段），忽略拦截，防止生成 = NULL
                if (currentTenantId == null) return true;
                // 4. 如果是平台超管（tenantId=0）且没指定目标，忽略拦截（查全部数据）
                if (currentTenantId == 0L) return true;

                // 5. 普通租户，强制拦截
                return false;
            }
        }));

        // ================= 2. 数据权限插件 =================
        interceptor.addInnerInterceptor(new DataPermissionInterceptor(new DataPermissionHandler() {
            @Override
            public Expression getSqlSegment(Expression where, String mappedStatementId) {
                // 【核心修正】：如果是 C端(/app/) 请求，直接忽略数据权限拦截！
                if (isAppRequest()) {
                    return null; // 返回 null 表示不追加任何数据权限 SQL 条件
                }

                Long userId = UserContext.getUserId();
                Long deptId = UserContext.getDeptId();
                Integer dataScope = UserContext.getDataScope();

                if (userId == null || UserContext.isSuperAdmin() || dataScope == null || dataScope == 1) {
                    return null;
                }

                // ... 原有的数据权限拼接逻辑 (仅本人、本部门等) 保持不变 ...
                if (dataScope == 4) {
                    return new net.sf.jsqlparser.expression.operators.relational.EqualsTo(
                            new net.sf.jsqlparser.schema.Column("create_by"), new LongValue(userId));
                }
                // ... 其他 dataScope 逻辑 ...

                return null;
            }
        }));

        // 3. 分页插件 (必须放在最后)
        PaginationInnerInterceptor paginationInterceptor = new PaginationInnerInterceptor(DbType.MYSQL);
        paginationInterceptor.setMaxLimit(500L);
        // 溢出处理：当请求的页码大于总页数时，是否回调到第一页 (true: 回调到第一页, false: 返回空数据)
        paginationInterceptor.setOverflow(false);
        // 优化 count 语句：对于简单的 left join 查询，自动优化 count sql (移除不必要的 order by 等)
        paginationInterceptor.setOptimizeJoin(true);
        interceptor.addInnerInterceptor(paginationInterceptor);

        return interceptor;
    }
}



