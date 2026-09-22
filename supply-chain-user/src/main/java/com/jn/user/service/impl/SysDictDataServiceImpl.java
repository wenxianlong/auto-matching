package com.jn.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jn.common.context.UserContext;
import com.jn.common.utils.RedisUtils;
import com.jn.user.entity.SysDictData;
import com.jn.user.mapper.SysDictDataMapper;
import com.jn.user.service.SysDictDataService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class SysDictDataServiceImpl extends ServiceImpl<SysDictDataMapper, SysDictData> implements SysDictDataService {

    private final RedisUtils redisUtils;
    private static final String DICT_CACHE_PREFIX = "sys:dict:";

    // 【核心】：缓存 Key 必须带上 tenantId，防止租户A读到租户B的缓存
    private String getCacheKey(String dictType) {
        return DICT_CACHE_PREFIX + UserContext.getTenantId() + ":" + dictType;
    }


    @Override
    public List<SysDictData> getDataByType(String dictType) {
//        String cacheKey = getCacheKey(dictType);
//
//        // 1. 尝试从 Redis 获取
//        List<SysDictData> cachedList = redisUtils.get(cacheKey, List.class);
//        if (cachedList != null && !cachedList.isEmpty()) {
//            return cachedList;
//        }

        Long currentTenantId = UserContext.getTenantId();
        // 2. 缓存未命中，查库 (当前租户私有 + 平台公共)
        List<SysDictData> dictList = this.list(new LambdaQueryWrapper<SysDictData>()
                .eq(SysDictData::getDictType, dictType)
                .eq(SysDictData::getStatus, 1)
                .and(w -> w.eq(SysDictData::getTenantId, currentTenantId)
                        .or().eq(SysDictData::getTenantId, 0L))
                .orderByAsc(SysDictData::getSortOrder));

        // 3. 写入 Redis
//        redisUtils.set(cacheKey, dictList, 24, TimeUnit.HOURS);
        return dictList;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void saveData(SysDictData data) {
        // 新增时自动注入租户ID
        if (data.getTenantId() == null) {
            data.setTenantId(UserContext.getTenantId());
        }
        this.saveOrUpdate(data);
        clearCache(data.getDictType());
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void removeData(Long id) {
        SysDictData data = this.getById(id);
        if (data != null) {
            this.removeById(id);
            clearCache(data.getDictType());
        }
    }

    public void clearCache(String dictType) {
        redisUtils.delete(DICT_CACHE_PREFIX + dictType);
    }
}

