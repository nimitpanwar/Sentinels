/**
 * AsyncConfig
 * 
 * PURPOSE: Configures the thread pool for asynchronous/background task execution.
 *          This is required for @Async methods to run on separate threads
 *          instead of blocking the main application thread.
 * 
 * WHAT IT SETS UP:
 *   - Core pool size: 4 threads minimum
 *   - Max pool size: 10 threads maximum
 *   - Queue capacity: 100 pending tasks
 *   - Thread naming: All threads named 'sentinel-async-*' for easy identification
 * 
 * WHY IT'S NEEDED:
 *   - TransactionEventListener uses @Async to run on a background thread
 *   - Without this config, there's no thread pool and async won't work properly
 *   - This ensures rule evaluation doesn't block transaction creation
 * 
 * HOW IT WORKS:
 *   - When a method is marked @Async, Spring automatically routes it to this
 *     thread pool instead of running it on the main thread
 *   - If all 10 threads are busy, new tasks wait in the queue (up to 100)
 *   - If queue is full, the task is rejected (Spring handles this)
 * 
 * TUNING: If you see performance issues later:
 *   - More threads = more concurrent rule evaluations
 *   - Larger queue = more buffering capacity
 *   - Adjust based on load testing results
 */
package com.example.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(6);
        executor.setMaxPoolSize(16);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("sentinel-async-");
        executor.initialize();
        return executor;
    }
}
