package spring.springserver

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@EnableScheduling
@SpringBootApplication
class SpringServerApplication

fun main(args: Array<String>) {
    runApplication<SpringServerApplication>(*args)
}
