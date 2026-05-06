package spring.springserver.domain.member.entity

import com.l98293.phone.Phone
import com.l98293.phone.Region
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id

@Entity
class Member (
    @Column(unique = true)
    var username: String,

    var name: String,

    @Column(unique = true)
    var email: String,

    @field:Phone(region = Region.KR)
    var phone: String,

    var password: String,

    @Enumerated(EnumType.STRING)
    var role: Role
) {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private var id: Long? = null

   fun getId(): Long? = id
}