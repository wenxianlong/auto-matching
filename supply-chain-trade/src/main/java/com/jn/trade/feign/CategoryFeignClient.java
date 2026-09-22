package com.jn.trade.feign;

import com.jn.common.dto.CategoryFieldConfigDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import java.util.List;

// 调用 config-service 的接口
@FeignClient(name = "config-service", path = "/api/categories")
public interface CategoryFeignClient {

    @GetMapping("/{categoryId}/fields")
    List<CategoryFieldConfigDTO> getFieldConfigs(@PathVariable("categoryId") Long categoryId);
}

