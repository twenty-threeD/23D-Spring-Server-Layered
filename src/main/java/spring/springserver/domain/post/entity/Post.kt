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

    @Column(nullable = false, length = 200)
    var title: String,

    @Column(length = 2000)
    var content: String?,

    var view_count: Int = 0,

    @Column(nullable = false, updatable = false)
    var updated_at: LocalDateTime?,

    var is_updated: Boolean = false,

    var is_deleted: Boolean = false,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    var member: Member?
) {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null


    @PrePersist
    fun prePersist() {

        updated_at = LocalDateTime.now()
    }

    @PreUpdate
    fun preUpdate() {

        updated_at = LocalDateTime.now()
    }
}
