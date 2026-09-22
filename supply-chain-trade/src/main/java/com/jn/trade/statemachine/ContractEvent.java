package com.jn.trade.statemachine;

public enum ContractEvent {
    SUBMIT,     // 提交审批
    APPROVE,    // 审批通过
    REJECT,     // 审批驳回
    COMPLETE,   // 履约完成
    CANCEL      // 取消合同
}

