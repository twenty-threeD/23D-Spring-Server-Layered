package spring.springserver.domain.member.entity

import com.l98293.phone.Format
import com.l98293.phone.Phone
import com.l98293.phone.Region
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.PrePersist
import java.time.LocalDateTime

@Entity
class Member(
    @Column(unique = true)
    var username: String,

    var name: String,

    @Column(unique = true)
    var email: String,

    @field:Phone(
        region = Region.KR,
        format = Format.LOCAL
    )
    @Column(nullable = true, unique = true)
    var phone: String?,

    var password: String?,

    @Enumerated(EnumType.STRING)
    var role: Role,

    @Enumerated(EnumType.STRING)
    var provider: Provider
) {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private var id: Long? = null

    @Column(
        nullable = false,
        columnDefinition = "boolean not null default false"
    )
    private var phoneVerified: Boolean = false

    private var createdAt: LocalDateTime? = null

    @PrePersist
    fun prePersistDate() {

        createdAt = LocalDateTime.now()
    }

   fun getId(): Long? = id

    fun isPhoneVerified(): Boolean = phoneVerified

    fun verifyPhone(phone: String) {

        this.phone = phone
        this.phoneVerified = true
    }

    fun changeEmail(email: String) {

        this.email = email
    }

    fun changePhone(phone: String) {

        this.phone = phone
        this.phoneVerified = true
    }

    fun update(name: String) { this.name = name }
}
