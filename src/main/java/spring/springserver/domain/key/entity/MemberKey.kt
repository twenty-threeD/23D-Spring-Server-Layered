package spring.springserver.domain.key.entity

import jakarta.persistence.*
import spring.springserver.domain.member.entity.Member

@Entity
class MemberKey(

    @Id
    private val id: Long? = null,

    @MapsId
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private val member: Member,

    @Column(length = 512)
    private var privateKey: String,

    @Column(length = 512)
    private val publicKey: String
) {

    fun getId() = id
    fun getPrivateKey(): String = privateKey
    fun getPublicKey(): String = publicKey

    fun updatePrivateKey(
        privateKey: String
    ) {

        this.privateKey = privateKey
    }
}