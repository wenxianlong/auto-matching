package com.jn.trade.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.jn.trade.entity.BizContract;

import java.util.Map;

public interface BizContractService extends IService<BizContract> {
    // 基础 CRUD 已由 IService 提供，若有复杂业务逻辑（如审批流转）可在此扩展
    Long createContract(Long categoryId, Long supplierId, Map<String, Object> dynamicFields);
    void submitForApproval(Long contractId);
    void approveContract(Long contractId);
}

