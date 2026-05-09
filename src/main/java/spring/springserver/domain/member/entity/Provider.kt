package spring.springserver.domain.member.entity

import spring.springserver.domain.auth.exception.AuthStatusCode
import spring.springserver.global.exception.exception.ApplicationException

enum class Provider {

    AUTH,
    GOOGLE;

    companion object {

        fun getRegistrationId(registrationId: String): Provider {

            return when(registrationId.lowercase()) {

                "google" -> GOOGLE
                else -> throw ApplicationException(AuthStatusCode.UNKNOWN_REGISTRATION_ID)
            }
        }
    }
}