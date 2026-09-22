package com.jn.trade.config;

import com.alibaba.druid.support.jakarta.StatViewServlet;
import com.alibaba.druid.support.jakarta.WebStatFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 手动注册 Druid 监控面板 (解决自动配置失效导致的 404 问题)
 */
@Configuration
public class DruidMonitorConfig {

    /**
     * 注册 Druid 监控 Servlet
     */
    @Bean
    public ServletRegistrationBean<StatViewServlet> druidStatViewServlet() {
        // 映射路径为 /druid/*
        ServletRegistrationBean<StatViewServlet> registrationBean =
                new ServletRegistrationBean<>(new StatViewServlet(), "/druid/*");

        // 设置登录账号密码
        registrationBean.addInitParameter("loginUsername", "admin");
        registrationBean.addInitParameter("loginPassword", "123456");
        // 是否允许重置数据
        registrationBean.addInitParameter("resetEnable", "false");
        // IP 白名单 (空表示允许所有)
        registrationBean.addInitParameter("allow", "");

        return registrationBean;
    }

    /**
     * 注册 Druid Web 统计 Filter
     */
    @Bean
    public FilterRegistrationBean<WebStatFilter> druidWebStatFilter() {
        FilterRegistrationBean<WebStatFilter> registrationBean =
                new FilterRegistrationBean<>(new WebStatFilter());

        // 拦截所有请求
        registrationBean.addUrlPatterns("/*");
        // 排除静态资源和 Druid 自身的监控页面
        registrationBean.addInitParameter("exclusions", "*.js,*.gif,*.jpg,*.png,*.css,*.ico,/druid/*");

        return registrationBean;
    }
}
