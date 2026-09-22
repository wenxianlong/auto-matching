package com.jn.trade.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.jn.trade.entity.BizCategory;
import com.jn.trade.entity.BizCategoryField;
import java.util.List;

public interface BizCategoryService extends IService<BizCategory> {

    /**
     * 获取指定品类下的所有字段配置
     */
    List<BizCategoryField> getFieldsByCategoryId(Long categoryId);

    /**
     * 保存品类的字段配置（全量覆盖：先删后插）
     */
    void saveFields(Long categoryId, List<BizCategoryField> fields);
}

