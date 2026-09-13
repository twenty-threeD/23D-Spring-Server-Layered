package spring.springserver.domain.contract.entity

import jakarta.persistence.*
import spring.springserver.domain.member.entity.Member
import java.time.LocalDateTime

@Entity
@Table(name = "contract")
class Contract(

    /**
     * 계약 당사자 갑(의뢰인). 용역을 의뢰하고 대금을 지급하는 쪽이다.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
        name = "party_a_id",
        nullable = false,
        foreignKey = ForeignKey(name = "fk_contract_client")
    )
    var client: Member,

    /**
     * 계약 당사자 을(전문가). 용역을 제공하고 대금을 받는 쪽이다.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
        name = "party_b_id",
        nullable = false,
        foreignKey = ForeignKey(name = "fk_contract_professional")
    )
    var professional: Member,

    /**
     * 계약서를 등록한 당사자다. 의뢰인·전문가 중 한쪽이며 요청 본문으로 받지 않고 로그인 정보에서 채운다.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
        name = "writer_id",
        nullable = false,
        foreignKey = ForeignKey(name = "fk_contract_writer")
    )
    var writer: Member,

    /**
     * 용역 시작 시각. 아직 정하지 않았으면 비워 둔다.
     */
    @Column(name = "started_at")
    var startedAt: LocalDateTime?,

    /**
     * 용역 종료 시각. 아직 정하지 않았으면 비워 둔다.
     */
    @Column(name = "ended_at")
    var endedAt: LocalDateTime?,

    /**
     * 용역 완료 후 의뢰인이 결과물을 검수하는 기간(일).
     */
    @Column(name = "inspection_period", nullable = false)
    var inspectionPeriod: Int,

    /**
     * 계약 금액(원). 의뢰인(갑)이 전문가(을)에게 지급하기로 한 용역 대금이다.
     */
    @Column(name = "price", nullable = false)
    var price: Long,

    /**
     * 계약 대상 용역의 내용.
     */
    @Column(name = "services_description", nullable = false, length = 2000)
    var servicesDescription: String
) {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private var id: Long? = null

    fun getId(): Long? = id

    fun isParty(
        memberId: Long?
    ): Boolean = client.getId() == memberId || professional.getId() == memberId
}
