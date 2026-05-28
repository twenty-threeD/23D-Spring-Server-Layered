package spring.springserver.domain.member.entity

import spring.springserver.domain.auth.exception.AuthStatusCode
import spring.springserver.global.exception.exception.ApplicationException

enum class Provider {

    AUTH,
    GOOGLE,
    KAKAO,
    NAVER;

    companion object {

        fun getRegistrationId(registrationId: String): Provider {

            return when(registrationId.lowercase()) {

                "google" -> GOOGLE
                "kakao" -> KAKAO
                "naver" -> NAVER
                else -> throw ApplicationException(AuthStatusCode.UNKNOWN_REGISTRATION_ID)
            }
        }
    }
}