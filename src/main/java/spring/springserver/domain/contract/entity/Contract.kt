package spring.springserver.domain.contract.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
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
@Table(name = "contract")
class Contract(

    /**
     * 계약 당사자 갑. 용역을 의뢰하고 대금을 지급하는 쪽이다.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "part_a_id", nullable = false)
    var partA: Member,

    /**
     * 계약 당사자 을. 용역을 제공하고 대금을 받는 쪽이다.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "part_b_id", nullable = false)
    var partB: Member,

    /**
     * 계약서를 등록한 당사자다. 갑·을 중 한쪽이며 서명이 붙기 전까지 계약서를 고칠 수 있다.
     * 요청 본문으로 받지 않고 로그인 정보에서 채운다.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "writer_id", nullable = false)
    var writer: Member,

    /**
     * 파일 업로드 API가 돌려준 계약서 PDF의 경로다.
     */
    @Column(name = "contract_url", nullable = false, length = 2048)
    var contractUrl: String,

    @Column(nullable = false)
    var amount: Long
) {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private var id: Long? = null

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private var status: ContractStatus = ContractStatus.DRAFT

    @Column(name = "part_a_signed_at")
    private var partASignedAt: LocalDateTime? = null

    @Column(name = "part_b_signed_at")
    private var partBSignedAt: LocalDateTime? = null

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
    fun getPartASignedAt(): LocalDateTime? = partASignedAt
    fun getPartBSignedAt(): LocalDateTime? = partBSignedAt

    fun isSigned() = status == ContractStatus.SIGNED
    fun isPartASigned() = partASignedAt != null
    fun isPartBSigned() = partBSignedAt != null

    /**
     * 한쪽이라도 서명했으면 계약 내용이 고정되어야 한다.
     */
    fun hasAnySignature() = isPartASigned() || isPartBSigned()

    fun update(
        contractUrl: String,
        amount: Long
    ) {

        this.contractUrl = contractUrl
        this.amount = amount
    }

    fun signAsPartA() {

        partASignedAt = LocalDateTime.now()

        markSignedIfCompleted()
    }

    fun signAsPartB() {

        partBSignedAt = LocalDateTime.now()

        markSignedIfCompleted()
    }

    /**
     * 양측 서명이 모두 모인 시점에만 계약이 성립한 것으로 본다.
     */
    private fun markSignedIfCompleted() {

        if (isPartASigned() && isPartBSigned()) {

            status = ContractStatus.SIGNED
        }
    }
}
