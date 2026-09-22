package com.jn.trade.statemachine;

public enum ContractState {
    DRAFT,              // 草稿
    PENDING_APPROVAL,   // 待审批
    ACTIVE,             // 已生效
    COMPLETED,          // 已完成
    CANCELLED           // 已取消
}

