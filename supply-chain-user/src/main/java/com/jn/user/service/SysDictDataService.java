package com.jn.user.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.jn.user.entity.SysDictData;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface SysDictDataService extends IService<SysDictData> {
    List<SysDictData> getDataByType(String dictType);

    @Transactional(rollbackFor = Exception.class)
    void saveData(SysDictData data);

    @Transactional(rollbackFor = Exception.class)
    void removeData(Long id);
}
