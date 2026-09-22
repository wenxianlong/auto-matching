package com.jn.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jn.common.context.UserContext;
import com.jn.common.utils.RedisUtils;
import com.jn.user.entity.SysDictType;
import com.jn.user.mapper.SysDictTypeMapper;
import com.jn.user.service.SysDictTypeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class SysDictTypeServiceImpl extends ServiceImpl<SysDictTypeMapper, SysDictType> implements SysDictTypeService {

    private final RedisUtils redisUtils;
    private static final String DICT_CACHE_KEY = "sys:dict:";

    @Override
    public List<SysDictType> getDictByType(String dictType) {
        String cacheKey = DICT_CACHE_KEY + dictType;

        // 1. 尝试从 Redis 获取
        List<SysDictType> cachedList = redisUtils.get(cacheKey, List.class);
        if (cachedList != null && !cachedList.isEmpty()) {
            return cachedList;
        }

        // 2. 缓存未命中，查库
        List<SysDictType> dictList = this.list(new LambdaQueryWrapper<SysDictType>()
                .eq(SysDictType::getDictType, dictType)
                .eq(SysDictType::getStatus, 1)
                .orderByAsc(SysDictType::getId));

        // 3. 写入 Redis (过期时间设为 24 小时)
        redisUtils.set(cacheKey, dictList, 24, TimeUnit.HOURS);
        return dictList;
    }

    @Override
    public List<SysDictType> listTypes() {
        Long currentTenantId = UserContext.getTenantId();
        // 【核心】：查询当前租户的私有字典 + 平台公共字典(tenant_id=0)
        return this.list(new LambdaQueryWrapper<SysDictType>()
                .and(w -> w.eq(SysDictType::getTenantId, currentTenantId)
                        .or().eq(SysDictType::getTenantId, 0L))
                .orderByDesc(SysDictType::getCreateTime));
    }

    @Override
    public boolean saveType(SysDictType type) {
        // 【核心】：新增时，如果没有指定租户，自动注入当前登录用户的租户ID
        if (type.getTenantId() == null) {
            type.setTenantId(UserContext.getTenantId());
        }
        return this.saveOrUpdate(type);
    }
}
