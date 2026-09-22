package com.jn.gateway.filter;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import java.util.List;

@Data
@Component // 注册为 Spring Bean
@ConfigurationProperties(prefix = "gateway.auth") // 绑定 yml 中的 gateway.auth 前缀
public class GatewayAuthProperties {

    /**
     * 白名单路径列表 (自动映射 yml 中的 white-list 数组)
     */
    private List<String> whiteList;

    /**
     * RSA 公钥字符串
     */
    private String publicKey;
}
