package spring.springserver.global.handler

import jakarta.validation.ConstraintViolationException
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.authentication.DisabledException
import org.springframework.security.authentication.LockedException
import org.springframework.validation.FieldError
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.MissingServletRequestParameterException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import org.springframework.web.servlet.resource.NoResourceFoundException
import spring.springserver.domain.auth.exception.AuthStatusCode
import spring.springserver.global.data.BaseResponse
import spring.springserver.global.data.ErrorResponse
import spring.springserver.global.exception.exception.ApplicationException
import spring.springserver.global.exception.status_code.CommonStatusCode

@RestControllerAdvice
class GlobalExceptionHandler {
    @ExceptionHandler(ApplicationException::class)
    fun handleApplicationException(
        applicationException: ApplicationException
    ): ResponseEntity<BaseResponse<Void>> {
        val statusCode = applicationException.statusCode
        val error = ErrorResponse.of(
            statusCode.getCode(),
            applicationException.message ?: statusCode.getMessage()
        )

        return ResponseEntity
            .status(statusCode.getHttpStatus())
            .body(BaseResponse.error(statusCode.getHttpStatus(), error))
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationException(
        methodArgumentNotValidException: MethodArgumentNotValidException
    ): ResponseEntity<BaseResponse<Void>> {
        val details = mutableMapOf<String, String>()

        methodArgumentNotValidException.bindingResult.allErrors.forEach { error ->
            val field = (error as FieldError).field
            val message = error.defaultMessage ?: "잘못된 입력값 입니다."
            details[field] = message
        }

        val error = ErrorResponse.of(
            CommonStatusCode.INVALID_ARGUMENT.getCode(),
            "요청값이 유효하지 않습니다.",
            details
        )

        return invalidArgument(error)
    }

    @ExceptionHandler(ConstraintViolationException::class)
    fun handleConstraintViolationException(
        constraintViolationException: ConstraintViolationException
    ): ResponseEntity<BaseResponse<Void>> {
        val details = mutableMapOf<String, String>()

        constraintViolationException.constraintViolations.forEach { violation ->
            details[violation.propertyPath.toString()] = violation.message
        }

        val error = ErrorResponse.of(
            CommonStatusCode.INVALID_ARGUMENT.getCode(),
            "요청값이 유효하지 않습니다.",
            details
        )

        return invalidArgument(error)
    }

    @ExceptionHandler(MissingServletRequestParameterException::class)
    fun handleMissingServletRequestParameterException(
        missingServletRequestParameterException: MissingServletRequestParameterException
    ): ResponseEntity<BaseResponse<Void>> {
        val error = ErrorResponse.of(
            CommonStatusCode.INVALID_ARGUMENT.getCode(),
            "필수 파라미터가 누락되었습니다: ${missingServletRequestParameterException.parameterName}"
        )

        return invalidArgument(error)
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException::class)
    fun handleMethodArgumentTypeMismatchException(
        methodArgumentTypeMismatchException: MethodArgumentTypeMismatchException
    ): ResponseEntity<BaseResponse<Void>> {
        val error = ErrorResponse.of(
            CommonStatusCode.INVALID_ARGUMENT.getCode(),
            "파라미터 타입이 잘못되었습니다: ${methodArgumentTypeMismatchException.name}"
        )

        return invalidArgument(error)
    }

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgumentException(
        illegalArgumentException: IllegalArgumentException
    ): ResponseEntity<BaseResponse<Void>> {
        val error = ErrorResponse.of(
            CommonStatusCode.INVALID_ARGUMENT.getCode(),
            illegalArgumentException.message ?: "잘못된 요청 파라미터입니다."
        )

        return invalidArgument(error)
    }

    @ExceptionHandler(BadCredentialsException::class)
    fun handleBadCredentialsException(
        badCredentialsException: BadCredentialsException
    ): ResponseEntity<BaseResponse<Void>> {
        val error = ErrorResponse.of(
            AuthStatusCode.INVALID_CREDENTIALS.getCode(),
            AuthStatusCode.INVALID_CREDENTIALS.getMessage()
        )

        return ResponseEntity
            .status(AuthStatusCode.INVALID_CREDENTIALS.getHttpStatus())
            .body(BaseResponse.error(AuthStatusCode.INVALID_CREDENTIALS.getHttpStatus(), error))
    }

    @ExceptionHandler(LockedException::class)
    fun handleAccountLockedException(
        lockedException: LockedException
    ): ResponseEntity<BaseResponse<Void>> {
        val error = ErrorResponse.of(
            AuthStatusCode.ACCOUNT_LOCKED.getCode(),
            AuthStatusCode.ACCOUNT_LOCKED.getMessage()
        )

        return ResponseEntity
            .status(AuthStatusCode.ACCOUNT_LOCKED.getHttpStatus())
            .body(BaseResponse.error(AuthStatusCode.ACCOUNT_LOCKED.getHttpStatus(), error))
    }

    @ExceptionHandler(DisabledException::class)
    fun handleAccountDisabledException(
        disabledException: DisabledException
    ): ResponseEntity<BaseResponse<Void>> {
        val error = ErrorResponse.of(
            AuthStatusCode.ACCOUNT_DISABLED.getCode(),
            AuthStatusCode.ACCOUNT_DISABLED.getMessage()
        )

        return ResponseEntity
            .status(AuthStatusCode.ACCOUNT_DISABLED.getHttpStatus())
            .body(BaseResponse.error(AuthStatusCode.ACCOUNT_DISABLED.getHttpStatus(), error))
    }

    @ExceptionHandler(NoResourceFoundException::class)
    fun handleNoResourceFoundException(
        noResourceFoundException: NoResourceFoundException
    ): ResponseEntity<BaseResponse<Void>> {
        val error = ErrorResponse.of(
            CommonStatusCode.ENDPOINT_NOT_FOUND.getCode(),
            CommonStatusCode.ENDPOINT_NOT_FOUND.getMessage()
        )

        return ResponseEntity
            .status(CommonStatusCode.ENDPOINT_NOT_FOUND.getHttpStatus())
            .body(BaseResponse.error(CommonStatusCode.ENDPOINT_NOT_FOUND.getHttpStatus(), error))
    }

    @ExceptionHandler(Exception::class)
    fun handleException(exception: Exception): ResponseEntity<BaseResponse<Void>> {
        log.error("요청 처리 중 에러 발생: {}", exception.message)

        val error = ErrorResponse.of(
            CommonStatusCode.INTERNAL_SERVER_ERROR.getCode(),
            CommonStatusCode.INTERNAL_SERVER_ERROR.getMessage()
        )

        return ResponseEntity
            .status(CommonStatusCode.INTERNAL_SERVER_ERROR.getHttpStatus())
            .body(BaseResponse.error(CommonStatusCode.INTERNAL_SERVER_ERROR.getHttpStatus(), error))
    }

    private fun invalidArgument(error: ErrorResponse): ResponseEntity<BaseResponse<Void>> {
        return ResponseEntity
            .status(CommonStatusCode.INVALID_ARGUMENT.getHttpStatus())
            .body(
                BaseResponse.error(
                    CommonStatusCode.INVALID_ARGUMENT.getHttpStatus(),
                    error
                )
            )
    }

    companion object {
        private val log = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)
    }
}
