package com.jn.trade.validator;

import cn.hutool.core.util.StrUtil;
import com.jn.common.dto.CategoryFieldConfigDTO;
import com.jn.trade.feign.CategoryFeignClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class DynamicFieldValidator {

    private final CategoryFeignClient categoryFeignClient;

    public void validate(Long categoryId, Map<String, Object> dynamicFields) {
        // 1. 通过 Feign 调用 config-service 获取字段配置
        List<CategoryFieldConfigDTO> configs = categoryFeignClient.getFieldConfigs(categoryId);
        if (configs == null || configs.isEmpty()) return;

        // 2. 遍历配置进行校验
        for (CategoryFieldConfigDTO config : configs) {
            Object value = dynamicFields == null ? null : dynamicFields.get(config.getFieldKey());

            // 必填校验
            if (Boolean.TRUE.equals(config.getIsRequired()) && (value == null || StrUtil.isBlank(value.toString()))) {
                throw new RuntimeException(config.getErrorMsg() != null ? config.getErrorMsg() : config.getFieldName() + "不能为空");
            }

            if (value != null) {
                String strValue = value.toString();
                // 长度校验
                if (config.getMaxLength() != null && strValue.length() > config.getMaxLength()) {
                    throw new RuntimeException(config.getFieldName() + "长度不能超过" + config.getMaxLength());
                }
                // 类型校验 (简单演示 NUMBER 类型)
                if ("NUMBER".equals(config.getFieldType())) {
                    try {
                        Double.parseDouble(strValue);
                    } catch (NumberFormatException e) {
                        throw new RuntimeException(config.getFieldName() + "必须是数字");
                    }
                }
            }
        }
    }
}

