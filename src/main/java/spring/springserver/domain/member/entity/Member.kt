package spring.springserver.domain.member.entity

import com.l98293.phone.Format
import com.l98293.phone.Phone
import com.l98293.phone.Region
import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
class Member(
    @Column(unique = true)
    var username: String,

    var name: String,

    @Column(unique = true)
    var email: String,

    @field:Phone(
        region = Region.KR,
        format = Format.LOCAL
    )
    @Column(nullable = true, unique = true)
    var phone: String?,

    var password: String?,

    @Enumerated(EnumType.STRING)
    var role: Role,

    @Enumerated(EnumType.STRING)
    var provider: Provider
) {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private var id: Long? = null

    @Column(
        nullable = false,
        columnDefinition = "boolean not null default false"
    )
    private var phoneVerified: Boolean = false

    private var createdAt: LocalDateTime? = null

    /**
     * 탈퇴 시각. null이 아니면 탈퇴한 회원이며 로그인할 수 없다.
     * 계약·견적 같은 거래 기록을 남겨야 해서 행 자체는 지우지 않는다.
     */
    @Column(name = "deleted_at")
    private var deletedAt: LocalDateTime? = null

    @PrePersist
    fun prePersistDate() {

        createdAt = LocalDateTime.now()
    }

   fun getId(): Long? = id

    fun isPhoneVerified(): Boolean = phoneVerified

    fun verifyPhone(phone: String) {

        this.phone = phone
        this.phoneVerified = true
    }

    fun changeEmail(email: String) {

        this.email = email
    }

    fun changePhone(phone: String) {

        this.phone = phone
        this.phoneVerified = true
    }

    fun update(name: String) { this.name = name }

    fun getDeletedAt(): LocalDateTime? = deletedAt

    fun isDeleted(): Boolean = deletedAt != null

    /**
     * 게시글·댓글 작성자로 노출할 이름.
     * 탈퇴 회원은 익명화된 식별자(deleted_1 등)가 그대로 보이지 않도록
     * 항상 "탈퇴한 사용자"로 낮춰 응답한다.
     */
    fun getDisplayName(): String {

        return if (isDeleted()) WITHDRAWN_DISPLAY_NAME else name
    }

    /**
     * 작성자 표시용 username. 탈퇴 회원이면 getDisplayName()과 같은 값이다.
     */
    fun getDisplayUsername(): String {

        return if (isDeleted()) WITHDRAWN_DISPLAY_NAME else username
    }

    /**
     * 탈퇴 처리. 행은 남기되 로그인에 쓰이는 식별자와 개인정보를 익명화한다.
     * username·email·phone에 unique 제약이 있어 값을 비워두면 재가입이 막히므로
     * 회원 id를 섞은 값으로 치환한다.
     */
    fun withdraw() {

        val suffix = id ?: System.currentTimeMillis()

        this.username = "deleted_$suffix"
        this.name = WITHDRAWN_DISPLAY_NAME
        this.email = "deleted_$suffix@deleted.local"
        this.phone = null
        this.password = null
        this.phoneVerified = false
        this.deletedAt = LocalDateTime.now()
    }

    companion object {

        const val WITHDRAWN_DISPLAY_NAME = "탈퇴한 사용자"
    }
}
