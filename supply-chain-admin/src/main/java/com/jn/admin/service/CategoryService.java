package com.jn.admin.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.jn.admin.entity.Category;
import com.jn.admin.entity.CategoryFieldConfig;
import com.jn.admin.entity.dto.CategoryCreateDTO;
import com.jn.admin.entity.dto.FieldConfigSaveDTO;
import com.jn.common.PageResult;
import com.jn.common.dto.CategoryQueryDTO;

import java.util.List;

public interface CategoryService extends IService<Category> {

    // 新增：分页查询品类列表
    PageResult<Category> pageCategories(CategoryQueryDTO queryDTO);

    void createCategory(CategoryCreateDTO dto);
    List<CategoryFieldConfig> getFieldConfigs(Long categoryId);
    void saveFieldConfigs(Long categoryId, List<FieldConfigSaveDTO> dtos);
}


