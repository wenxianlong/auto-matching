package com.jn.trade.statemachine;

import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.config.EnableStateMachineFactory;
import org.springframework.statemachine.config.StateMachineConfigurerAdapter;
import org.springframework.statemachine.config.builders.StateMachineStateConfigurer;
import org.springframework.statemachine.config.builders.StateMachineTransitionConfigurer;

import java.util.EnumSet;

@Configuration
@EnableStateMachineFactory(name = "contractStateMachineFactory")
public class ContractStateMachineConfig extends StateMachineConfigurerAdapter<ContractState, ContractEvent> {

    @Override
    public void configure(StateMachineStateConfigurer<ContractState, ContractEvent> states) throws Exception {
        states.withStates()
                .initial(ContractState.DRAFT)
                .end(ContractState.COMPLETED)
                .end(ContractState.CANCELLED)
                .states(EnumSet.allOf(ContractState.class));
    }

    @Override
    public void configure(StateMachineTransitionConfigurer<ContractState, ContractEvent> transitions) throws Exception {
        transitions
                // 草稿 -> 待审批
                .withExternal().source(ContractState.DRAFT).target(ContractState.PENDING_APPROVAL).event(ContractEvent.SUBMIT)
                .and()
                // 待审批 -> 已生效
                .withExternal().source(ContractState.PENDING_APPROVAL).target(ContractState.ACTIVE).event(ContractEvent.APPROVE)
                .and()
                // 待审批 -> 草稿 (驳回)
                .withExternal().source(ContractState.PENDING_APPROVAL).target(ContractState.DRAFT).event(ContractEvent.REJECT)
                .and()
                // 已生效 -> 已完成
                .withExternal().source(ContractState.ACTIVE).target(ContractState.COMPLETED).event(ContractEvent.COMPLETE)
                .and()
                // 已生效 -> 已取消
                .withExternal().source(ContractState.ACTIVE).target(ContractState.CANCELLED).event(ContractEvent.CANCEL);
    }
}

