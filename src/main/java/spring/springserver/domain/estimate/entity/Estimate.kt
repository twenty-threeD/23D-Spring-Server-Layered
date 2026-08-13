package spring.springserver.domain.estimate.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.PrePersist
import jakarta.persistence.Table
import org.hibernate.annotations.UpdateTimestamp
import spring.springserver.domain.member.entity.Member
import java.time.LocalDateTime

@Entity
@Table(name = "estimate")
class Estimate(

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    var client: Member,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "professional_id", nullable = false)
    var professional: Member,

    @Column(nullable = false, length = 500)
    var url: String,

    @Column(name = "total_pay", nullable = false)
    var totalPay: Long,

    @Column(nullable = false)
    var paid: Boolean = false
) {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private var id: Long? = null

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

    fun update(
        url: String,
        totalPay: Long
    ) {

        this.url = url
        this.totalPay = totalPay
    }

    fun markAsPaid() {

        this.paid = true
    }
}
