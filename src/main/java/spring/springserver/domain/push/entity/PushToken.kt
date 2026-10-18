package spring.springserver.domain.push.entity

import jakarta.persistence.*
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import spring.springserver.domain.member.entity.Member
import java.time.LocalDateTime

/**
 * 기기 하나의 Expo 푸시 토큰. 한 회원이 기기를 여러 대 쓰므로 회원당 여러 행이 생긴다.
 * 토큰은 기기에 묶인 값이라 전역에서 하나만 존재한다. 다른 계정으로 다시 등록되면 행의 회원만 바꾼다.
 */
@Entity
@Table(
    name = "push_token",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_push_token_token", columnNames = ["token"])
    ],
    indexes = [
        Index(name = "idx_push_token_member", columnList = "member_id")
    ]
)
class PushToken(

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    var member: Member,

    @Column(nullable = false, length = 255)
    val token: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var platform: PushPlatform
) {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private var id: Long? = null

    @CreationTimestamp
    private var createdAt: LocalDateTime? = null

    @UpdateTimestamp
    private var updatedAt: LocalDateTime? = null

    fun getId(): Long? = id

    fun reassign(
        member: Member,
        platform: PushPlatform
    ) {

        this.member = member
        this.platform = platform
    }
}
