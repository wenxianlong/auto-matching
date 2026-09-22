package com.jn.admin.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jn.admin.entity.Category;
import com.jn.admin.entity.CategoryFieldConfig;
import com.jn.admin.entity.dto.CategoryCreateDTO;
import com.jn.admin.entity.dto.FieldConfigSaveDTO;
import com.jn.admin.mapper.CategoryFieldConfigMapper;
import com.jn.admin.mapper.CategoryMapper;
import com.jn.admin.service.CategoryService;
import com.jn.common.BusinessException;
import com.jn.common.PageResult;
import com.jn.common.dto.CategoryQueryDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl extends ServiceImpl<CategoryMapper, Category> implements CategoryService {

    private final CategoryFieldConfigMapper fieldConfigMapper;

    @Override
    public PageResult<Category> pageCategories(CategoryQueryDTO queryDTO) {
        // 1. 构建分页对象
        Page<Category> page = new Page<>(queryDTO.getPageNum(), queryDTO.getPageSize());

        // 2. 构建查询条件
        LambdaQueryWrapper<Category> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StrUtil.isNotBlank(queryDTO.getName()), Category::getName, queryDTO.getName())
                .eq(StrUtil.isNotBlank(queryDTO.getCode()), Category::getCode, queryDTO.getCode())
                .orderByDesc(Category::getCreateTime); // 按创建时间倒序

        // 3. 执行分页查询 (MyBatis-Plus 分页插件会自动拦截并处理)
        Page<Category> resultPage = this.page(page, wrapper);

        // 4. 转换为自定义的 PageResult
        return PageResult.of(
                resultPage.getTotal(),
                resultPage.getPages(),
                resultPage.getCurrent(),
                resultPage.getSize(),
                resultPage.getRecords()
        );
    }

    @Override
    public void createCategory(CategoryCreateDTO dto) {
        // 校验编码唯一性 (多租户拦截器会自动加上 tenant_id 条件)
        long count = this.count(new LambdaQueryWrapper<Category>().eq(Category::getCode, dto.getCode()));
        if (count > 0) {
            throw new BusinessException("品类编码已存在: " + dto.getCode());
        }

        Category category = new Category();
        BeanUtils.copyProperties(dto, category);
        this.save(category);
    }

    @Override
    public List<CategoryFieldConfig> getFieldConfigs(Long categoryId) {
        return fieldConfigMapper.selectList(
                new LambdaQueryWrapper<CategoryFieldConfig>()
                        .eq(CategoryFieldConfig::getCategoryId, categoryId)
                        .orderByAsc(CategoryFieldConfig::getSortOrder)
        );
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveFieldConfigs(Long categoryId, List<FieldConfigSaveDTO> dtos) {
        Category category = this.getById(categoryId);
        if (category == null) throw new BusinessException("品类不存在");

        fieldConfigMapper.delete(
                new LambdaQueryWrapper<CategoryFieldConfig>().eq(CategoryFieldConfig::getCategoryId, categoryId)
        );

        if (dtos != null && !dtos.isEmpty()) {
            long distinctKeys = dtos.stream().map(FieldConfigSaveDTO::getFieldKey).distinct().count();
            if (distinctKeys != dtos.size()) {
                throw new BusinessException("字段标识(fieldKey)不能重复");
            }

            List<CategoryFieldConfig> configs = dtos.stream().map(dto -> {
                CategoryFieldConfig config = new CategoryFieldConfig();
                BeanUtils.copyProperties(dto, config);
                config.setCategoryId(categoryId);
                return config;
            }).collect(Collectors.toList());

            configs.forEach(fieldConfigMapper::insert);
        }
    }
}



