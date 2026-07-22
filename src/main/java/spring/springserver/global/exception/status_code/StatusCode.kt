package spring.springserver.global.exception.status_code

import org.springframework.http.HttpStatus

interface StatusCode {
    fun getCode(): String

    fun getMessage(): String

    fun getHttpStatus(): HttpStatus
}
