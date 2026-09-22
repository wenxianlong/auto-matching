package com.jn.common.config;

import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JacksonConfig {

    /**
     * 全局配置：将 Long 类型序列化为 String，防止前端 JavaScript 精度丢失
     */
    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jackson2ObjectMapperBuilderCustomizer() {
        return builder -> {
            // 处理包装类 Long
            builder.serializerByType(Long.class, ToStringSerializer.instance);
            // 处理基本类型 long
            builder.serializerByType(Long.TYPE, ToStringSerializer.instance);
        };
    }
}

