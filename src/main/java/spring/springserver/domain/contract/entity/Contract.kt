package spring.springserver.domain.contract.entity

import jakarta.persistence.*
import org.hibernate.annotations.UpdateTimestamp
import spring.springserver.domain.member.entity.Member
import java.time.LocalDateTime

@Entity
@Table(name = "contract")
class Contract(

    /**
     * 계약 당사자 갑(의뢰인). 용역을 의뢰하고 대금을 지급하는 쪽이다.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    var client: Member,

    /**
     * 계약 당사자 을(전문가). 용역을 제공하고 대금을 받는 쪽이다.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "professional_id", nullable = false)
    var professional: Member,

    /**
     * 계약서를 등록한 당사자다. 의뢰인·전문가 중 한쪽이며 서명이 붙기 전까지 계약서를 고칠 수 있다.
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

    /**
     * 계약 금액(원). 의뢰인(갑)이 전문가(을)에게 지급하기로 한 용역 대금이다.
     */
    @Column(nullable = false)
    var price: Long
) {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private var id: Long? = null

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private var status: ContractStatus = ContractStatus.DRAFT

    @Column(name = "client_signed_at")
    private var clientSignedAt: LocalDateTime? = null

    @Column(name = "professional_signed_at")
    private var professionalSignedAt: LocalDateTime? = null

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
    fun getClientSignedAt(): LocalDateTime? = clientSignedAt
    fun getProfessionalSignedAt(): LocalDateTime? = professionalSignedAt

    fun isSigned() = status == ContractStatus.SIGNED
    fun isClientSigned() = clientSignedAt != null
    fun isProfessionalSigned() = professionalSignedAt != null

    /**
     * 한쪽이라도 서명했으면 계약 내용이 고정되어야 한다.
     */
    fun hasAnySignature() = isClientSigned() || isProfessionalSigned()

    fun update(
        contractUrl: String,
        price: Long
    ) {

        this.contractUrl = contractUrl
        this.price = price
    }

    fun signAsClient() {

        clientSignedAt = LocalDateTime.now()

        markSignedIfCompleted()
    }

    fun signAsProfessional() {

        professionalSignedAt = LocalDateTime.now()

        markSignedIfCompleted()
    }

    /**
     * 양측 서명이 모두 모인 시점에만 계약이 성립한 것으로 본다.
     */
    private fun markSignedIfCompleted() {

        if (isClientSigned() && isProfessionalSigned()) {

            status = ContractStatus.SIGNED
        }
    }
}
