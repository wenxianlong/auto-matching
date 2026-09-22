package com.jn.user.controller;

import com.jn.common.Result;
import com.jn.user.entity.SysDictData;
import com.jn.user.entity.SysDictType;
import com.jn.user.service.SysDictDataService;
import com.jn.user.service.SysDictTypeService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/admin/system/dict")
@RequiredArgsConstructor
public class SysDictController {

    private final SysDictTypeService typeService;
    private final SysDictDataService dataService;

    // ================= 字典类型接口 =================

    @GetMapping("/type/list")
    @PreAuthorize("hasAuthority('sys:dict:list') or hasAuthority('*:*:*')")
    public Result<List<SysDictType>> listTypes() {
        return Result.success(typeService.list());
    }

    @PostMapping("/type")
    @PreAuthorize("hasAuthority('sys:dict:add') or hasAuthority('*:*:*')")
    public Result<Void> addType(@RequestBody SysDictType type) {
        typeService.save(type);
        return Result.success();
    }

    @PutMapping("/type")
    @PreAuthorize("hasAuthority('sys:dict:edit') or hasAuthority('*:*:*')")
    public Result<Void> editType(@RequestBody SysDictType type) {
        typeService.updateById(type);
        return Result.success();
    }

    @DeleteMapping("/type/{id}")
    @PreAuthorize("hasAuthority('sys:dict:remove') or hasAuthority('*:*:*')")
    public Result<Void> removeType(@PathVariable Long id) {
        typeService.removeById(id);
        return Result.success();
    }

    // ================= 字典数据接口 =================

    @GetMapping("/data/list")
    public Result<List<SysDictData>> listData(@RequestParam String dictType) {
        return Result.success(dataService.getDataByType(dictType));
    }

    @PostMapping("/data")
    @PreAuthorize("hasAuthority('sys:dict:add') or hasAuthority('*:*:*')")
    public Result<Void> addData(@RequestBody SysDictData data) {
        dataService.saveData(data);
        return Result.success();
    }

    @PutMapping("/data")
    @PreAuthorize("hasAuthority('sys:dict:edit') or hasAuthority('*:*:*')")
    public Result<Void> editData(@RequestBody SysDictData data) {
        dataService.saveData(data);
        return Result.success();
    }

    @DeleteMapping("/data/{id}")
    @PreAuthorize("hasAuthority('sys:dict:remove') or hasAuthority('*:*:*')")
    public Result<Void> removeData(@PathVariable Long id) {
        dataService.removeData(id);
        return Result.success();
    }

    /**
     * 根据字典类型获取字典数据
     */
    @GetMapping("/data/{dictType}")
    public Result<List<SysDictType>> getDictData(@PathVariable String dictType) {
        return Result.success(typeService.getDictByType(dictType));
    }
}

