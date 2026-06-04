package spring.springserver.domain.key.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.MapsId
import jakarta.persistence.OneToOne
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
    private val privateKey: String,

    @Column(length = 512)
    private val publicKey: String
)