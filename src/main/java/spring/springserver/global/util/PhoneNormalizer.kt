package spring.springserver.global.util

import spring.springserver.global.exception.exception.ApplicationException
import spring.springserver.global.exception.status_code.CommonStatusCode

object PhoneNormalizer {

    private val LOCAL_NUMBER = Regex("^0\\d{8,10}$")

    fun normalize(
        phone: String
    ): String {

        val normalized = phone.filter { it.isDigit() }

        if (!LOCAL_NUMBER.matches(normalized)) {

            throw ApplicationException(CommonStatusCode.INVALID_ARGUMENT)
        }

        return normalized
    }
}