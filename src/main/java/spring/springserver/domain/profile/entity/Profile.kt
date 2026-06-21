package spring.springserver.domain.profile.entity

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
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import org.hibernate.annotations.UpdateTimestamp
import spring.springserver.domain.jobcategory.entity.JobCategory
import spring.springserver.domain.location.entity.Sig
import spring.springserver.domain.member.entity.Member
import java.time.LocalDateTime

@Entity
@Table(name = "profile")
class Profile(

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false, unique = true)
    var member: Member,

    @Column(length = 500)
    var imageUrl: String? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sig_cd")
    var sig: Sig? = null,

    @Enumerated(EnumType.STRING)
    var movableDistance: MovableDistance? = null,

    @Column(length = 100)
    var shortDescription: String? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_category_id")
    var jobCategory: JobCategory? = null
) {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private val id: Long? = null

    @UpdateTimestamp
    private var updatedAt: LocalDateTime? = null

    fun getId(): Long? = id

    fun getUpdatedAt(): LocalDateTime? = updatedAt

    fun update(
        imageUrl: String?,
        sig: Sig?,
        movableDistance: MovableDistance?,
        shortDescription: String?,
        jobCategory: JobCategory?
    ) {

        this.imageUrl = imageUrl
        this.sig = sig
        this.movableDistance = movableDistance
        this.shortDescription = shortDescription
        this.jobCategory = jobCategory
    }
}