package spring.springserver.domain.post.entity

import jakarta.persistence.Column
import jakarta.persistence.CascadeType
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import spring.springserver.domain.member.entity.Member
import java.time.LocalDateTime

@Entity
class Post (
    @Column(nullable = false, length = 255)
    var title: String,

    @Column(length = 2000)
    var content: String,

    var viewCount: Int = 0,

    @Column(nullable = false)
    var updatedAt: LocalDateTime,

    var isEdited: Boolean = false,

    var isDeleted: Boolean = false,

    var deletedAt: LocalDateTime? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_member_id", nullable = false)
    var member: Member,

    @OneToMany(mappedBy = "post", cascade = [CascadeType.ALL], orphanRemoval = true)
    var attachments: MutableList<PostAttach> = mutableListOf()
) {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private var id: Long? = null

    fun getId(): Long? = id

    fun preUpdate() {

        this.updatedAt = LocalDateTime.now()
    }

    fun addAttachment(fileUrl: String) {

        attachments.add(PostAttach(fileUrl = fileUrl, post = this))
    }
}
