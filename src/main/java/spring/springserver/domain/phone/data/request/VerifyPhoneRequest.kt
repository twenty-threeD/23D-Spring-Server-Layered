package spring.springserver.domain.phone.data.request

data class VerifyPhoneRequest(
    val recipientNumber: String,
    val code: String
)
