package spring.springserver.domain.post.favorite.service

import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import spring.springserver.domain.auth.exception.AuthStatusCode
import spring.springserver.domain.member.entity.Member
import spring.springserver.domain.member.repository.MemberRepository
import spring.springserver.domain.post.data.response.PostResponse
import spring.springserver.domain.post.entity.Post
import spring.springserver.domain.post.exception.PostStatusCode
import spring.springserver.domain.post.favorite.data.response.PostFavoriteResponse
import spring.springserver.domain.post.favorite.entity.PostFavorite
import spring.springserver.domain.post.favorite.repository.PostFavoriteRepository
import spring.springserver.domain.post.repository.PostRepository
import spring.springserver.global.exception.exception.ApplicationException
import spring.springserver.global.exception.status_code.CommonStatusCode

@Service
@Transactional(rollbackFor = [Exception::class])
class PostFavoriteServiceImpl(
    private val postFavoriteRepository: PostFavoriteRepository,
    private val postRepository: PostRepository,
    private val memberRepository: MemberRepository
): PostFavoriteService {

    override fun favoritePost(
        postId: Long
    ): PostFavoriteResponse {

        val member = getCurrentMember()

        val post = getActivePost(postId)

        if (postFavoriteRepository.existsByMemberAndPost(member, post)) {

            throw alreadyFavoritePostException()
        }

        try {
            postFavoriteRepository.saveAndFlush(
                PostFavorite(
                    member = member,
                    post = post,
                )
            )
        } catch (exception: DataIntegrityViolationException) {

            throw alreadyFavoritePostException()
        }

        return PostFavoriteResponse.of(
            postId,
            postFavoriteRepository.countByPostId(postId),
            "게시글 찜이 등록되었습니다.",
        )
    }

    override fun unfavoritePost(
        postId: Long
    ): PostFavoriteResponse {

        val member = getCurrentMember()

        val post = getActivePost(postId)

        val deletedCount = postFavoriteRepository.deleteByMemberAndPost(member, post)

        if (deletedCount == 0L) {

            throw ApplicationException.of(
                CommonStatusCode.INVALID_ARGUMENT,
                "찜하지 않은 게시글입니다."
            )
        }

        return PostFavoriteResponse.of(
            postId,
            postFavoriteRepository.countByPostId(postId),
            "게시글 찜이 취소되었습니다.",
        )
    }

    @Transactional(readOnly = true)
    override fun viewFavoritePosts(
        pageable: Pageable
    ): Page<PostResponse> {

        val member = getCurrentMember()

        return postFavoriteRepository.findFavoritePostsByMember(
            member,
            pageable.withoutSort()
        ).map { post -> PostResponse.of(post) }
    }

    private fun getCurrentMember() = SecurityContextHolder.getContext().authentication?.name
        ?.takeIf { username -> username.isNotBlank() && username != "anonymousUser" }
        ?.let { username -> memberRepository.findByUsername(username) }
        ?: throw ApplicationException(AuthStatusCode.USERNAME_NOT_FOUND)

    private fun getActivePost(postId: Long): Post {

        val post = postRepository.findPostById(postId)
            ?: throw ApplicationException(PostStatusCode.INVALID_POST)

        if (post.isDeleted) {

            throw ApplicationException(PostStatusCode.INVALID_POST)
        }

        return post
    }

    private fun alreadyFavoritePostException() = ApplicationException.of(
        CommonStatusCode.INVALID_ARGUMENT,
        "이미 찜한 게시글입니다."
    )

    private fun Pageable.withoutSort(): Pageable =
        PageRequest.of(pageNumber, pageSize)
}
