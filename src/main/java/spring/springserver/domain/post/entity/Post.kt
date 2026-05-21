package spring.springserver.domain.post.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.PrePersist
import jakarta.persistence.PreUpdate
import spring.springserver.domain.member.entity.Member
import java.time.LocalDateTime

@Entity
class Post (
    @Column(nullable = false, length = 255)
    var title: String,

    @Column(length = 2000)
    var content: String?,

    var viewCount: Int = 0,

    @Column(nullable = false)
    var updatedAt: LocalDateTime?,

    var isUpdated: Boolean = false,

    var isDeleted: Boolean = false,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_member_id", nullable = false)
    var member: Member?
) {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null

}
