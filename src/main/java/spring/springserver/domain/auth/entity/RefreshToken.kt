package spring.springserver.domain.auth.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.PrePersist
import spring.springserver.domain.member.entity.Member
import java.time.LocalDateTime

@Entity
class RefreshToken(

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private val member: Member,

    @Column(unique = true, length = 512)
    private var token: String,

    @Column(nullable = false)
    private var expiresAt: LocalDateTime
) {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private var id: Long? = null

    private var createdAt: LocalDateTime? = null

    @PrePersist
    fun prePersistDate() {

        createdAt = LocalDateTime.now()
    }

    fun getId(): Long? = id
    fun getMember(): Member = member
    fun getToken(): String = token
    fun getExpiresAt(): LocalDateTime = expiresAt

    fun isExpired(): Boolean = expiresAt.isBefore(LocalDateTime.now())

    fun update(
        token: String,
        expiresAt: LocalDateTime
    ) {

        this.token = token
        this.expiresAt = expiresAt
    }
}
