package spring.springserver.domain.key.service

interface KeyService {

    fun generateKeyPair(memberId: Long)

    fun signHash(
        memberId: Long,
        hash: String
    ): String

    fun deriveCosmosAddress(
        memberId: Long
    ): String

    fun getPublicKeyBase64(
        memberId: Long
    ): String
}