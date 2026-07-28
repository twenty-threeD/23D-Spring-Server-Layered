package spring.springserver.global.exception.exception

import spring.springserver.global.exception.status_code.StatusCode

class ApplicationException(
    val statusCode: StatusCode,
    message: String = statusCode.getMessage(),
    throwable: Throwable? = null
) : RuntimeException(message, throwable) {

    constructor(statusCode: StatusCode, throwable: Throwable) : this(
        statusCode = statusCode,
        message = statusCode.getMessage(),
        throwable = throwable
    )

    companion object {

        fun of(
            statusCode: StatusCode
        ): ApplicationException {

            return ApplicationException(statusCode)
        }

        fun of(
            statusCode: StatusCode,
            throwable: Throwable
        ): ApplicationException {

            return ApplicationException(statusCode, throwable)
        }

        fun of(
            statusCode: StatusCode,
            customMessage: String
        ): ApplicationException {

            return ApplicationException(statusCode, customMessage)
        }
    }
}
