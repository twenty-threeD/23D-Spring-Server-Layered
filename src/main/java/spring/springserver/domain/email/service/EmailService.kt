package spring.springserver.domain.email.service

import spring.springserver.domain.email.data.response.CheckVerifyCodeResponse
import spring.springserver.domain.email.data.response.SendVerifyCodeResponse

interface EmailService {

    fun sendVerifyCode(email: String,
                       clientIp: String): SendVerifyCodeResponse

    fun checkVerifyCode(email: String,
                        code: String): CheckVerifyCodeResponse

    fun sendChangeEmailCode(email: String,
                            clientIp: String): SendVerifyCodeResponse

    fun checkChangeEmailCode(email: String,
                             code: String): CheckVerifyCodeResponse

    /**
     * 가입 인증을 마친 이메일이면 그 표식을 소비하고 true를 돌려준다.
     * 표식은 인증코드 확인에 성공하면 남고, 한 번 쓰면 사라진다.
     * 다만 표식을 소비한 트랜잭션이 롤백되면 남은 TTL로 되살려, 가입 저장 실패 때문에 인증을 다시 받지 않게 한다.
     */
    fun consumeEmailVerification(email: String): Boolean
}