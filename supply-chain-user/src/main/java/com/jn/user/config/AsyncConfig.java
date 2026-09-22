package com.jn.user.config;

import com.jn.common.context.UserContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskDecorator;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean("taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(20);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("async-task-");

        // 【核心】：设置任务装饰器，在异步线程执行前复制 ThreadLocal 上下文
        executor.setTaskDecorator(new ContextCopyingDecorator());
        executor.initialize();
        return executor;
    }

    private static class ContextCopyingDecorator implements TaskDecorator {
        @Override
        public Runnable decorate(Runnable runnable) {
            // 在主线程中获取上下文
            Long tenantId = UserContext.getTenantId();
            Long userId = UserContext.getUserId();

            return () -> {
                try {
                    // 在异步线程中恢复上下文
                    UserContext.setTenantId(tenantId);
                    UserContext.setUserId(userId);
                    runnable.run();
                } finally {
                    // 执行完毕后清理，防止线程池复用导致内存泄漏
                    UserContext.clear();
                }
            };
        }
    }
}

