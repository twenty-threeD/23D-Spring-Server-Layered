package spring.springserver.domain.post.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.PrePersist
import jakarta.persistence.PreUpdate
import jakarta.persistence.Transient
import spring.springserver.domain.member.entity.Member
import java.time.LocalDateTime

@Entity
class Post (

    @Column(nullable = false)
    var title: String,

    var content: String? = null,

    var image_url: String? = null,

    var view_count: Int = 0,

    @Column(nullable = false, updatable = false)
    var created_at: LocalDateTime? = null,

    var updated_at: LocalDateTime? = null
) {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null

    @Transient
    lateinit var member: Member

    @PrePersist
    fun prePersist() {

        val now = LocalDateTime.now()
        created_at = now
        updated_at = null
    }

    @PreUpdate
    fun preUpdate() {

        updated_at = LocalDateTime.now()
    }
}
