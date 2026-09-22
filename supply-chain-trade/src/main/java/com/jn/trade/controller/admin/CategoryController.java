package com.jn.trade.controller.admin;

import com.jn.common.Result;
import com.jn.trade.entity.BizCategory;
import com.jn.trade.entity.BizCategoryField;
import com.jn.trade.service.BizCategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/admin/business/category")
@RequiredArgsConstructor
public class CategoryController {

    private final BizCategoryService categoryService;

    @GetMapping("/list")
    @PreAuthorize("hasAuthority('trade:category:list') or hasAuthority('*:*:*')")
    public Result<List<BizCategory>> list() {
        return Result.success(categoryService.list());
    }

    @PostMapping
    @PreAuthorize("hasAuthority('trade:category:add') or hasAuthority('*:*:*')")
    public Result<Void> add(@RequestBody BizCategory category) {
        categoryService.save(category);
        return Result.success();
    }

    // 获取某品类的字段配置
    @GetMapping("/{categoryId}/fields")
    public Result<List<BizCategoryField>> getFields(@PathVariable(name = "categoryId") Long categoryId) {
        return Result.success(categoryService.getFieldsByCategoryId(categoryId));
    }

    // 保存某品类的字段配置 (全量覆盖)
    @PostMapping("/{categoryId}/fields")
    @PreAuthorize("hasAuthority('trade:category:configField') or hasAuthority('*:*:*')")
    public Result<Void> saveFields(@PathVariable Long categoryId, @RequestBody List<BizCategoryField> fields) {
        categoryService.saveFields(categoryId, fields);
        return Result.success();
    }
}

