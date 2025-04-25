package com.sdumagicode.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
public class ThreadPoolConfig {
    
    @Bean("embeddingExecutor")
    public ExecutorService embeddingExecutor() {
        // 根据实际需求设置线程数，这里设置为CPU核心数*2
        int threadCount = Runtime.getRuntime().availableProcessors() * 2;
        return Executors.newFixedThreadPool(threadCount);
    }
}
