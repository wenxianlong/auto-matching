package com.jn.admin.controller;

import com.jn.admin.entity.Category;
import com.jn.admin.entity.CategoryFieldConfig;
import com.jn.admin.entity.dto.CategoryCreateDTO;
import com.jn.admin.entity.dto.FieldConfigSaveDTO;
import com.jn.admin.service.CategoryService;
import com.jn.common.PageResult;
import com.jn.common.Result;
import com.jn.common.dto.CategoryQueryDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    /**
     * 分页查询品类列表 (支持按名称模糊搜索)
     */
    @GetMapping("/page")
    public Result<PageResult<Category>> pageCategories(CategoryQueryDTO queryDTO) {
        return Result.success(categoryService.pageCategories(queryDTO));
    }

    /**
     * 获取当前租户的所有品类 (不分页，通常用于下拉框数据源)
     */
    @GetMapping("/list")
    public Result<List<Category>> listAllCategories() {
        return Result.success(categoryService.list());
    }

    /**
     * 新增品类
     */
    @PostMapping
    public Result<Void> createCategory(@Valid @RequestBody CategoryCreateDTO dto) {
        categoryService.createCategory(dto);
        return Result.success();
    }

    /**
     * 删除品类
     */
    @DeleteMapping("/{id}")
    public Result<Void> deleteCategory(@PathVariable Long id) {
        categoryService.removeById(id);
        return Result.success();
    }

    /**
     * 获取指定品类的动态字段配置
     */
    @GetMapping("/{categoryId}/fields")
    public Result<List<CategoryFieldConfig>> getFieldConfigs(@PathVariable Long categoryId) {
        return Result.success(categoryService.getFieldConfigs(categoryId));
    }

    /**
     * 保存/更新指定品类的动态字段配置
     */
    @PostMapping("/{categoryId}/fields")
    public Result<Void> saveFieldConfigs(@PathVariable Long categoryId,
                                         @Valid @RequestBody List<FieldConfigSaveDTO> dtos) {
        categoryService.saveFieldConfigs(categoryId, dtos);
        return Result.success();
    }
}



