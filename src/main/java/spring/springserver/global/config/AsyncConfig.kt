package spring.springserver.global.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import java.util.concurrent.Executor
import java.util.concurrent.ThreadPoolExecutor

/**
 * `@Async`용 실행기. 기본 SimpleAsyncTaskExecutor는 호출마다 스레드를 새로 만들어
 * 팬아웃처럼 한 번에 여러 건이 몰리는 작업에는 쓰지 않는다.
 *
 * 큐가 가득 차면 호출 스레드에서 그대로 실행해(CallerRuns) 알림을 버리지 않는다.
 * 기본 정책은 AbortPolicy라서 TaskRejectedException이 나는데, 반환 타입이 Unit인
 * `@Async` 호출에서는 아무도 받지 못하고 알림이 조용히 사라진다.
 *
 * 큐를 작게 잡는 것도 같은 이유다. ThreadPoolTaskExecutor는 큐가 가득 찬 뒤에야
 * core를 넘겨 스레드를 늘리므로, 큐가 크면 maxPoolSize에 영영 도달하지 않는다.
 */
@Configuration
@EnableAsync
class AsyncConfig {

    @Bean("notificationExecutor")
    fun notificationExecutor(): Executor {

        val threadPoolTaskExecutor = ThreadPoolTaskExecutor()

        threadPoolTaskExecutor.corePoolSize = 2
        threadPoolTaskExecutor.maxPoolSize = 4
        threadPoolTaskExecutor.queueCapacity = 50
        threadPoolTaskExecutor.setRejectedExecutionHandler(ThreadPoolExecutor.CallerRunsPolicy())
        threadPoolTaskExecutor.setThreadNamePrefix("notification-")
        threadPoolTaskExecutor.setWaitForTasksToCompleteOnShutdown(true)
        threadPoolTaskExecutor.setAwaitTerminationSeconds(30)
        threadPoolTaskExecutor.initialize()

        return threadPoolTaskExecutor
    }
}