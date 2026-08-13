package spring.springserver.domain.payment.repository

import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import spring.springserver.domain.payment.entity.Payment
import spring.springserver.domain.payment.entity.PaymentStatus
import java.time.LocalDateTime

interface PaymentRepository: JpaRepository<Payment, Long> {

    fun findByOrderId(orderId: String): Payment?

    fun existsByOrderId(orderId: String): Boolean

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select payment from Payment payment where payment.orderId = :orderId")
    fun findByOrderIdForUpdate(@Param("orderId") orderId: String): Payment?

    @Query(
        "select payment from Payment payment " +
                "where payment.status = :status " +
                "and payment.cancelAttemptCount < :maxAttemptCount " +
                "order by payment.updatedAt asc"
    )
    fun findRetryableByStatus(
        @Param("status") status: PaymentStatus,
        @Param("maxAttemptCount") maxAttemptCount: Int
    ): List<Payment>

    @Query(
        "select payment from Payment payment " +
                "where payment.status = :status " +
                "and payment.updatedAt < :updatedBefore"
    )
    fun findStuckByStatus(
        @Param("status") status: PaymentStatus,
        @Param("updatedBefore") updatedBefore: LocalDateTime
    ): List<Payment>
}