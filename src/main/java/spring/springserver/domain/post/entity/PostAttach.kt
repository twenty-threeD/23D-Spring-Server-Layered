package spring.springserver.domain.post.entity

import jakarta.persistence.*

@Entity
class PostAttach(
    var fileUrl: String? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    var post: Post
) {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private var id: Long? = null

    fun getId() = id
}
