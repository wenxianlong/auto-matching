package com.jn.trade;

import com.alibaba.druid.spring.boot3.autoconfigure.DruidDataSourceAutoConfigure;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication(
        exclude = {
                DataSourceAutoConfiguration.class,       // 排除原生单数据源配置
                DruidDataSourceAutoConfigure.class       // 【核心】：排除原生 Druid 自动配置，交由 dynamic-datasource 接管
        }
)
@EnableDiscoveryClient
@EnableFeignClients // 开启 OpenFeign 微服务调用
@MapperScan("com.jn.trade.mapper")
public class TradeApplication {
    public static void main(String[] args) {
        SpringApplication.run(TradeApplication.class, args);
    }
}

