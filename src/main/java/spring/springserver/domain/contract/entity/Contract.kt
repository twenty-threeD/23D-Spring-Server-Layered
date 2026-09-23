package spring.springserver.domain.contract.entity

import jakarta.persistence.*
import spring.springserver.domain.member.entity.Member
import spring.springserver.domain.post.entity.Post
import java.time.LocalDateTime

@Entity
@Table(name = "contract")
class Contract(

    // 갑(대체로 용역 구매자)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
        name = "party_a_id",
        nullable = false,
        foreignKey = ForeignKey(name = "fk_contract_client")
    )
    var client: Member,

    // 을(대체로 능력자, 용역 판매자)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
        name = "party_b_id",
        nullable = false,
        foreignKey = ForeignKey(name = "fk_contract_professional")
    )
    var professional: Member,

    // 계약 시작일
    @Column(name = "started_at")
    var startedAt: LocalDateTime?,

    // 계약 만료일
    @Column(name = "ended_at")
    var endedAt: LocalDateTime?,

    // 계약 검수 기간
    @Column(name = "inspection_period", nullable = false)
    var inspectionPeriod: Int,

    // 계약 대금
    @Column(name = "price", nullable = false)
    var price: Long,

    // 용역의 내용
    @Column(name = "services_description", nullable = false, length = 2000)
    var servicesDescription: String,

    // 계약서 작성자
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
        name = "writer_id",
        nullable = false,
        foreignKey = ForeignKey(name = "fk_contract_writer")
    )
    var writer: Member,

    // 계약서 주소
    @Column(name = "contract_url", nullable = false, length = 2048)
    var contractUrl: String,

    /**
     * 계약의 출발점이 된 게시글. 게시글 없이 채팅으로만 맺은 계약도 있어 nullable이다.
     * 리뷰를 게시글 단위로 묶어 보여줄 때만 쓰고, 평점 집계는 professional 기준으로 한다.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
        name = "post_id",
        foreignKey = ForeignKey(name = "fk_contract_post")
    )
    var post: Post? = null
) {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private var id: Long? = null

    private var createdAt: LocalDateTime? = null

    @PrePersist
    fun prePersistDate() {

        createdAt = LocalDateTime.now()
    }

    fun getId(): Long? = id
    fun getCreatedAt(): LocalDateTime? = createdAt

    fun isParty(
        memberId: Long?
    ): Boolean = client.getId() == memberId || professional.getId() == memberId

    fun isClient(
        memberId: Long?
    ): Boolean = client.getId() == memberId
}
