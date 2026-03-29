package com.ast.back.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean("plagiarismJobExecutor")
    public Executor plagiarismJobExecutor(
            @Value("${app.plagiarism.executor.core-pool-size:2}") int corePoolSize,
            @Value("${app.plagiarism.executor.max-pool-size:4}") int maxPoolSize,
            @Value("${app.plagiarism.executor.queue-capacity:20}") int queueCapacity
    ) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setThreadNamePrefix("plagiarism-job-");
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }
}