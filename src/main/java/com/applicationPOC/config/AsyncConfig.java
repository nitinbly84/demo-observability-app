package com.applicationPOC.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Configuration class to set up asynchronous method execution.
 * Defines a custom thread pool executor for handling @Async methods.
 * This custom thread pool will be used instead of the default one provided by Spring.
 * If want to have multiple executors, define them as beans and use @Async("beanName") to specify which one to use.
 * As shown in com.applicationPOC.config.ConditionalConfig.java.
 */
@Configuration
public class AsyncConfig implements AsyncConfigurer {

    @Override
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor ex = new ThreadPoolTaskExecutor();
        ex.setCorePoolSize(2);
        ex.setMaxPoolSize(4);
        ex.setQueueCapacity(100);
        ex.setThreadNamePrefix("async-exec-");
        ex.initialize();
        return ex;
    }
}
