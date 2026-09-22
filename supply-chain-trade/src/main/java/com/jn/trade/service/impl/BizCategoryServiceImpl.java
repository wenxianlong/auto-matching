package com.jn.trade.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jn.trade.entity.BizCategory;
import com.jn.trade.entity.BizCategoryField;
import com.jn.trade.mapper.BizCategoryFieldMapper;
import com.jn.trade.mapper.BizCategoryMapper;
import com.jn.trade.service.BizCategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BizCategoryServiceImpl extends ServiceImpl<BizCategoryMapper, BizCategory> implements BizCategoryService {

    private final BizCategoryFieldMapper fieldMapper;

    @Override
    public List<BizCategoryField> getFieldsByCategoryId(Long categoryId) {
        return fieldMapper.selectList(new LambdaQueryWrapper<BizCategoryField>()
                .eq(BizCategoryField::getCategoryId, categoryId)
                .orderByAsc(BizCategoryField::getSortOrder));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveFields(Long categoryId, List<BizCategoryField> fields) {
        // 1. 物理删除该品类下的旧字段配置
        fieldMapper.delete(new LambdaQueryWrapper<BizCategoryField>()
                .eq(BizCategoryField::getCategoryId, categoryId));

        // 2. 批量插入新的字段配置
        if (fields != null && !fields.isEmpty()) {
            for (int i = 0; i < fields.size(); i++) {
                BizCategoryField field = fields.get(i);
                field.setId(null); // 确保重新生成ID
                field.setCategoryId(categoryId);
                field.setSortOrder(i + 1); // 自动修正排序号
                fieldMapper.insert(field);
            }
        }
    }
}

