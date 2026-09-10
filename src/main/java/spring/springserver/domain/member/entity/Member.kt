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

    /**
     * 익명화 시각. 탈퇴 후 재가입 제한 기간이 지나 식별자·개인정보를 치환한 시점이다.
     * null이 아니면 이 회원이 쥐고 있던 username·email·phone은 이미 풀려 재가입에 쓸 수 있다.
     */
    @Column(name = "anonymized_at")
    private var anonymizedAt: LocalDateTime? = null

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
     * 탈퇴 회원의 원본 값은 그대로 두고 응답에서만 "탈퇴한 사용자"로 낮춘다.
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
     * 탈퇴 처리. deletedAt만 남기고 원본 값은 덮어쓰지 않는다.
     * 로그인은 AuthServiceImpl·MemberDetailsService의 isDeleted() 검사로 막고,
     * 작성자 노출은 getDisplayName()·getDisplayUsername()으로 낮춘다.
     */
    fun withdraw() {

        this.deletedAt = LocalDateTime.now()
    }

    fun getAnonymizedAt(): LocalDateTime? = anonymizedAt

    fun isAnonymized(): Boolean = anonymizedAt != null

    /**
     * 탈퇴 후 재가입 제한 기간이 지났는지. threshold는 (현재 시각 - 제한 기간)이다.
     */
    fun isWithdrawnBefore(
        threshold: LocalDateTime
    ): Boolean {

        val deletedAt = this.deletedAt ?: return false

        return deletedAt.isBefore(threshold)
    }

    /**
     * 재가입 제한 기간이 지난 탈퇴 회원의 식별자·개인정보를 치환한다.
     * username·email·phone에 unique 제약이 있어 원래 값을 그대로 두면
     * 같은 이메일·전화번호로 다시 가입할 수 없으므로 회원 id를 섞은 값으로 비켜준다.
     * 계약·견적이 참조하는 행 자체는 그대로 남는다.
     */
    fun anonymize() {

        if (isAnonymized()) {

            return
        }

        val suffix = id ?: System.currentTimeMillis()

        this.username = "deleted_$suffix"
        this.name = WITHDRAWN_DISPLAY_NAME
        this.email = "deleted_$suffix@deleted.local"
        this.phone = null
        this.password = null
        this.phoneVerified = false
        this.anonymizedAt = LocalDateTime.now()
    }

    companion object {

        const val WITHDRAWN_DISPLAY_NAME = "탈퇴한 사용자"
    }
}
