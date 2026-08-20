package spring.springserver.domain.post.category.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table

@Entity
@Table(name = "post_category")
class PostCategory(

    @Column(nullable = false, length = 50)
    var name: String,

    @JoinColumn(name = "parent_id")
    @ManyToOne(fetch = FetchType.LAZY)
    var parent: PostCategory? = null
) {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private val id: Long? = null

    fun getId(): Long? = id

    /**
     * 상위 카테고리를 거슬러 올라가며 전체 이름을 만든다.
     * 이미 지나온 카테고리는 멈추므로, 데이터가 순환하더라도 무한 재귀가 되지 않는다.
     */
    fun getFullName(): String {

        val names = mutableListOf(name)
        val visited = mutableSetOf(this)
        var current = parent

        while (current != null && visited.add(current)) {

            names.add(current.name)
            current = current.parent
        }

        return names.reversed()
            .joinToString(" > ")
    }
}
