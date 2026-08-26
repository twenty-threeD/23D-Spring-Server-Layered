package spring.springserver.domain.estimate.entity

import jakarta.persistence.*
import org.hibernate.annotations.UpdateTimestamp
import spring.springserver.domain.member.entity.Member
import spring.springserver.domain.post.entity.Post
import java.time.LocalDateTime

@Entity
@Table(name = "estimate")
class Estimate(

    /**
     * 어떤 게시글에 대한 견적서인지 가리킨다.
     * 같은 전문가와 진행 중인 견적서가 여러 건일 수 있으므로,
     * 결제 대상을 특정하려면 게시글 기준으로 좁혀야 한다.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    var post: Post,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    var client: Member,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "professional_id", nullable = false)
    var professional: Member,

    @Column(nullable = false, length = 2048)
    var url: String,

    @Column(name = "total_pay", nullable = false)
    var totalPay: Long
) {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private var id: Long? = null

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private var status: EstimateStatus = EstimateStatus.PROPOSED

    private var createdAt: LocalDateTime? = null

    @UpdateTimestamp
    private var updatedAt: LocalDateTime? = null

    @PrePersist
    fun prePersistDate() {

        createdAt = LocalDateTime.now()
    }

    fun getId(): Long? = id
    fun getCreatedAt(): LocalDateTime? = createdAt
    fun getUpdatedAt(): LocalDateTime? = updatedAt
    fun getStatus() = status

    fun isPaid() = status == EstimateStatus.PAID

    fun update(
        url: String,
        totalPay: Long
    ) {

        this.url = url
        this.totalPay = totalPay
    }

    fun markAsPaid() {

        this.status = EstimateStatus.PAID
    }
}
