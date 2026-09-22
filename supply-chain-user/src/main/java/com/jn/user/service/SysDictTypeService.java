package com.jn.user.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.jn.user.entity.SysDictType;

import java.util.List;

public interface SysDictTypeService extends IService<SysDictType> {
    List<SysDictType> getDictByType(String dictType);

    List<SysDictType> listTypes();

    boolean saveType(SysDictType type);
}
