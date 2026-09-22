package com.jn.trade.service.impl;

import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jn.trade.context.UserContext;
import com.jn.trade.entity.BizContract;
import com.jn.trade.entity.DeliveryPlan;
import com.jn.trade.mapper.BizContractMapper;
import com.jn.trade.mapper.DeliveryPlanMapper;
import com.jn.trade.service.BizContractService;
import com.jn.trade.statemachine.ContractEvent;
import com.jn.trade.statemachine.ContractState;
import com.jn.trade.validator.DynamicFieldValidator;
import io.seata.spring.annotation.GlobalTransactional;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.StateMachineEventResult;
import org.springframework.statemachine.config.StateMachineFactory;
import org.springframework.statemachine.support.DefaultStateMachineContext;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class BizContractServiceImpl extends ServiceImpl<BizContractMapper, BizContract> implements BizContractService {

    private final DynamicFieldValidator dynamicFieldValidator;
    private final DeliveryPlanMapper deliveryPlanMapper;
    private final StateMachineFactory<ContractState, ContractEvent> contractStateMachineFactory;

    @Override
    @GlobalTransactional(name = "create-contract-and-delivery", rollbackFor = Exception.class) // Seata 分布式事务
    public Long createContract(Long categoryId, Long supplierId, Map<String, Object> dynamicFields) {
        // 1. 校验动态字段 (Feign 调用 config-service)
        dynamicFieldValidator.validate(categoryId, dynamicFields);

        // 2. 保存合同主表
        BizContract contract = new BizContract();
        contract.setContractNo("HT" + IdUtil.getSnowflakeNextIdStr());
        contract.setCategoryId(categoryId);
        contract.setExtraData(dynamicFields);
        contract.setStatus(ContractState.DRAFT.name());
        contract.setCreateBy(UserContext.getUserId());
        contract.setCreateTime(LocalDateTime.now());
        this.save(contract);

        // 3. 初始化收发货计划 (模拟跨库/跨服务操作，由 Seata 保证一致性)
        DeliveryPlan plan = new DeliveryPlan();
        plan.setContractId(contract.getId());
        plan.setPlanNo("DH" + IdUtil.getSnowflakeNextIdStr());
        plan.setStatus("INIT");
        deliveryPlanMapper.insert(plan);

        return contract.getId();
    }

    @Override
    public void submitForApproval(Long contractId) {
        changeState(contractId, ContractEvent.SUBMIT);
    }

    @Override
    public void approveContract(Long contractId) {
        changeState(contractId, ContractEvent.APPROVE);
    }

    /**
     * 状态机流转核心方法 (适配 Spring Statemachine 4.x 响应式 Flux API)
     */
    private void changeState(Long contractId, ContractEvent event) {
        BizContract contract = this.getById(contractId);
        if (contract == null) throw new RuntimeException("合同不存在");

        // 1. 获取状态机实例
        StateMachine<ContractState, ContractEvent> stateMachine = contractStateMachineFactory.getStateMachine(String.valueOf(contractId));

        // 2. 停止状态机并恢复当前状态 (从数据库读取)
        stateMachine.stopReactively().block();

        stateMachine.getStateMachineAccessor().doWithAllRegions(sma -> {
            sma.resetStateMachineReactively(
                    new DefaultStateMachineContext<>(ContractState.valueOf(contract.getStatus()), null, null, null)
            ).block();
        });

        stateMachine.startReactively().block();

        // 3. 构建消息并发送事件
        Message<ContractEvent> message = MessageBuilder.withPayload(event).build();

        // sendEvent 接收 Mono<Message>，返回 Flux<StateMachineEventResult>
        Flux<StateMachineEventResult<ContractState, ContractEvent>> resultFlux = stateMachine.sendEvent(Mono.just(message));

        // 使用 blockLast() 获取流中最后一个（最终决定性的）处理结果
        StateMachineEventResult<ContractState, ContractEvent> result = resultFlux.blockLast();

        // 4. 校验流转结果 (注意 ResultType 的正确路径是 StateMachineEventResult.ResultType)
        boolean success = result != null && result.getResultType() == StateMachineEventResult.ResultType.ACCEPTED;

        if (!success) {
            throw new RuntimeException("状态流转失败，当前状态不允许执行此操作，或状态机配置有误");
        }

        // 5. 获取新状态并更新数据库
        contract.setStatus(stateMachine.getState().getId().name());
        this.updateById(contract);
    }
}
