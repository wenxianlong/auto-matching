package com.jn.trade.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.jn.common.Result;
import com.jn.trade.entity.MfgResistor;
import com.jn.trade.mapper.MfgResistorMapper;
import com.jn.trade.service.GroupingAlgorithmService;
import com.jn.trade.service.MfgResistorService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/admin/business/grouping")
@RequiredArgsConstructor
public class GroupingController {

    // 【关键】：注入 Mapper，用于在 DB 层进行预筛选
    private final MfgResistorMapper resistorMapper;

    private final GroupingAlgorithmService algorithmService;

    @PostMapping("/calculate")
//    @DS("sqlserver")  如果使用sqlserver数据库，添加此注解
    public Result<List<GroupingAlgorithmService.GroupingResult>> calculate(@RequestBody GroupingAlgorithmService.GroupingRequest req) {
        // 【关键】：DB 层预筛选，只加载符合条件的候选片到内存
        LambdaQueryWrapper<MfgResistor> wrapper = algorithmService.buildFilterWrapper(req);
        List<MfgResistor> candidates = resistorMapper.selectList(wrapper);

        // 传入已过滤的候选集进行配组计算
        List<GroupingAlgorithmService.GroupingResult> results = algorithmService.calculate(candidates, req);
        return Result.success(results);
    }

//    // 【关键】：使用 @DSTransactional 替代 @Transactional
//    @DSTransactional 和 @Transactional 不要同时使用，且 @DSTransactional 内部不要捕获异常后吞掉，必须抛出才能触发回滚。
//    @DSTransactional
//    public void createOrderAndSyncErp(OrderDTO dto) {
//        // 1. 写入 MySQL
//        orderMapper.insert(new Order(dto));
//
//        // 2. 扣减 SQL Server 库存
//        erpMapper.deductStock(dto.getMaterialCode(), dto.getCount());
//
//        // 如果这里抛出异常，MySQL 和 SQL Server 都会回滚！
//        if (someErrorCondition) {
//            throw new RuntimeException("模拟异常");
//        }
//    }
}
