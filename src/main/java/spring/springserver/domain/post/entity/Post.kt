package spring.springserver.domain.post.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.ManyToOne
import jakarta.persistence.PrePersist
import jakarta.persistence.PreUpdate
import jakarta.persistence.Transient
import spring.springserver.domain.member.entity.Member
import java.time.LocalDateTime

@Entity
class Post (

    @Column(nullable = false, length = 200)
    var title: String,

    var content: String?,

    var image_url: String?,

    var view_count: Int = 0,

    @Column(nullable = false, updatable = false)
    var created_at: LocalDateTime,

    var updated_at: LocalDateTime?
) {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null

    @ManyToOne
    lateinit var member: Member

    @PrePersist
    fun prePersist() {

        created_at = LocalDateTime.now()
        updated_at = null
    }

    @PreUpdate
    fun preUpdate() {

        updated_at = LocalDateTime.now()
    }
}
