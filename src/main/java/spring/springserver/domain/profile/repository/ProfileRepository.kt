package spring.springserver.domain.profile.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import spring.springserver.domain.member.entity.Member
import spring.springserver.domain.profile.entity.Profile

interface ProfileRepository: JpaRepository<Profile, Long> {

    fun findByMember(
        member: Member
    ): Profile?

    fun existsByMember(
        member: Member
    ): Boolean

    /**
     * member_id가 profile의 FK이므로 join 없이 조회된다.
     * 목록 응답에서 회원별 프로필 이미지를 한 번에 가져오기 위해 사용한다.
     */
    @Query(
        """
        select p.member.id, p.imageUrl
        from Profile p
        where p.member.id in :memberIds
        """
    )
    fun findMemberImageUrls(
        @Param("memberIds") memberIds: Collection<Long>
    ): List<Array<Any?>>

    /**
     * 구인/구직 알림을 받을 회원의 username을 찾는다.
     * 프로필에 지역과 카테고리를 모두 설정한 회원만 대상이 되며, 글쓴이 자신은 뺀다.
     */
    @Query(
        """
        select p.member.username
        from Profile p
        where p.sig.sigCd in :sigCds
          and p.jobCategory.id in :jobCategoryIds
          and p.member.id <> :excludedMemberId
        """
    )
    fun findUsernamesBySigCdsAndJobCategoryIds(
        @Param("sigCds") sigCds: Collection<String>,
        @Param("jobCategoryIds") jobCategoryIds: Collection<Long>,
        @Param("excludedMemberId") excludedMemberId: Long
    ): List<String>
}