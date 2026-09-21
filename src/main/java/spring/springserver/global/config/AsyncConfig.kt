package spring.springserver.global.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import java.util.concurrent.Executor

/**
 * `@Async`용 실행기. 기본 SimpleAsyncTaskExecutor는 호출마다 스레드를 새로 만들어
 * 팬아웃처럼 한 번에 여러 건이 몰리는 작업에는 쓰지 않는다.
 *
 * 큐가 가득 차면 호출 스레드에서 그대로 실행해(CallerRuns) 알림을 버리지 않는다.
 */
@Configuration
@EnableAsync
class AsyncConfig {

    @Bean("notificationExecutor")
    fun notificationExecutor(): Executor {

        val threadPoolTaskExecutor = ThreadPoolTaskExecutor()

        threadPoolTaskExecutor.corePoolSize = 2
        threadPoolTaskExecutor.maxPoolSize = 4
        threadPoolTaskExecutor.queueCapacity = 500
        threadPoolTaskExecutor.setThreadNamePrefix("notification-")
        threadPoolTaskExecutor.setWaitForTasksToCompleteOnShutdown(true)
        threadPoolTaskExecutor.setAwaitTerminationSeconds(30)
        threadPoolTaskExecutor.initialize()

        return threadPoolTaskExecutor
    }
}