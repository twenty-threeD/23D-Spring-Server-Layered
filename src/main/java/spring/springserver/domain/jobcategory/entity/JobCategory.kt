package spring.springserver.domain.jobcategory.entity

import jakarta.persistence.*
import org.hibernate.annotations.BatchSize

/**
 * 카테고리 전체 이름을 만들 때 parent 체인을 타고 올라가므로,
 * 상위 카테고리를 하나씩 조회하지 않도록 배치로 묶어 읽는다.
 */
@Entity
@Table(name = "job_category")
@BatchSize(size = 100)
class JobCategory(

    @Column(nullable = false, length = 50)
    var name: String,

    @JoinColumn(name = "parent_id")
    @ManyToOne(fetch = FetchType.LAZY)
    var parent: JobCategory? = null
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
