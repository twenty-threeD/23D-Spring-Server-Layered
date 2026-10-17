package spring.springserver.domain.community.comment.service

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import spring.springserver.domain.community.comment.data.request.CreateCommentRequest
import spring.springserver.domain.community.comment.entity.CommunityComment
import spring.springserver.domain.community.comment.repository.CommunityCommentRepository
import spring.springserver.domain.community.comment.service.impl.CommunityCommentServiceImpl
import spring.springserver.domain.community.common.exception.CommunityStatusCode
import spring.springserver.domain.community.common.service.CommunityAuthorizationService
import spring.springserver.domain.community.post.entity.CommunityPost
import spring.springserver.domain.member.entity.Member
import spring.springserver.domain.profile.service.ProfileService
import spring.springserver.global.exception.exception.ApplicationException
import java.time.LocalDateTime

class CommunityCommentServiceImplTest {

    private val communityCommentRepository: CommunityCommentRepository = mock()
    private val communityAuthorizationService: CommunityAuthorizationService = mock()
    private val profileService: ProfileService = mock()

    private val service = CommunityCommentServiceImpl(
        communityCommentRepository,
        communityAuthorizationService,
        profileService
    )

    @Test
    fun `parent 가 없으면 일반 댓글로 저장되고 parent 는 null 이다`() {

        val member = buildMember(1L)
        val post = buildPost(10L)

        whenever(communityAuthorizationService.getCurrentMember()).thenReturn(member)
        whenever(communityAuthorizationService.getActivePost(10L)).thenReturn(post)
        whenever(communityCommentRepository.save(any())).thenAnswer { invocation -> invocation.arguments[0] }

        service.createComment(
            CreateCommentRequest(
                postId = 10L,
                content = "hi",
                parentId = null
            )
        )

        val captor = argumentCaptor<CommunityComment>()
        verify(communityCommentRepository).save(captor.capture())
        assertThat(captor.firstValue.parent).isNull()
    }

    @Test
    fun `parent 가 존재하고 같은 post 이면 답글로 저장된다`() {

        val member = buildMember(1L)
        val post = buildPost(10L)
        val parent = buildComment(id = 100L, post = post, parent = null, author = member)

        whenever(communityAuthorizationService.getCurrentMember()).thenReturn(member)
        whenever(communityAuthorizationService.getActivePost(10L)).thenReturn(post)
        whenever(communityCommentRepository.findByIdAndDeletedAtIsNull(100L)).thenReturn(parent)
        whenever(communityCommentRepository.save(any())).thenAnswer { invocation -> invocation.arguments[0] }

        service.createComment(
            CreateCommentRequest(
                postId = 10L,
                content = "reply",
                parentId = 100L
            )
        )

        val captor = argumentCaptor<CommunityComment>()
        verify(communityCommentRepository).save(captor.capture())
        assertThat(captor.firstValue.parent).isSameAs(parent)
    }

    @Test
    fun `parent 가 다른 post 의 댓글이면 400`() {

        val member = buildMember(1L)
        val requestedPost = buildPost(10L)
        val otherPost = buildPost(999L)
        val parent = buildComment(id = 100L, post = otherPost, parent = null, author = member)

        whenever(communityAuthorizationService.getCurrentMember()).thenReturn(member)
        whenever(communityAuthorizationService.getActivePost(10L)).thenReturn(requestedPost)
        whenever(communityCommentRepository.findByIdAndDeletedAtIsNull(100L)).thenReturn(parent)

        assertThatThrownBy {

            service.createComment(
                CreateCommentRequest(
                    postId = 10L,
                    content = "reply",
                    parentId = 100L
                )
            )
        }
            .isInstanceOfSatisfying(ApplicationException::class.java) { exception ->

                assertThat(exception.statusCode).isEqualTo(CommunityStatusCode.COMMENT_PARENT_POST_MISMATCH)
            }
    }

    @Test
    fun `parent 가 이미 답글이면 400`() {

        val member = buildMember(1L)
        val post = buildPost(10L)
        val rootComment = buildComment(id = 50L, post = post, parent = null, author = member)
        val replyComment = buildComment(id = 100L, post = post, parent = rootComment, author = member)

        whenever(communityAuthorizationService.getCurrentMember()).thenReturn(member)
        whenever(communityAuthorizationService.getActivePost(10L)).thenReturn(post)
        whenever(communityCommentRepository.findByIdAndDeletedAtIsNull(100L)).thenReturn(replyComment)

        assertThatThrownBy {

            service.createComment(
                CreateCommentRequest(
                    postId = 10L,
                    content = "nested",
                    parentId = 100L
                )
            )
        }
            .isInstanceOfSatisfying(ApplicationException::class.java) { exception ->

                assertThat(exception.statusCode).isEqualTo(CommunityStatusCode.COMMENT_REPLY_DEPTH_EXCEEDED)
            }
    }

    @Test
    fun `parent id 를 지정했는데 그 댓글이 없으면 400`() {

        val member = buildMember(1L)
        val post = buildPost(10L)

        whenever(communityAuthorizationService.getCurrentMember()).thenReturn(member)
        whenever(communityAuthorizationService.getActivePost(10L)).thenReturn(post)
        whenever(communityCommentRepository.findByIdAndDeletedAtIsNull(100L)).thenReturn(null)

        assertThatThrownBy {

            service.createComment(
                CreateCommentRequest(
                    postId = 10L,
                    content = "reply",
                    parentId = 100L
                )
            )
        }
            .isInstanceOfSatisfying(ApplicationException::class.java) { exception ->

                assertThat(exception.statusCode).isEqualTo(CommunityStatusCode.COMMENT_PARENT_NOT_FOUND)
            }
    }

    @Test
    fun `답글이 있는 원댓글 삭제는 soft delete 로만 처리된다`() {

        val member = buildMember(1L)
        val post = buildPost(10L)
        val parent = buildComment(id = 50L, post = post, parent = null, author = member)

        whenever(communityAuthorizationService.getCurrentMember()).thenReturn(member)
        whenever(communityAuthorizationService.getActiveComment(50L)).thenReturn(parent)
        whenever(communityCommentRepository.existsByParentId(50L)).thenReturn(true)

        service.deleteComment(50L)

        assertThat(parent.deletedAt).isNotNull()
        verify(communityCommentRepository, never()).delete(any())
    }

    @Test
    fun `답글이 없는 댓글 삭제는 hard delete 로 처리된다`() {

        val member = buildMember(1L)
        val post = buildPost(10L)
        val comment = buildComment(id = 50L, post = post, parent = null, author = member)

        whenever(communityAuthorizationService.getCurrentMember()).thenReturn(member)
        whenever(communityAuthorizationService.getActiveComment(50L)).thenReturn(comment)
        whenever(communityCommentRepository.existsByParentId(50L)).thenReturn(false)

        service.deleteComment(50L)

        verify(communityCommentRepository).delete(comment)
        assertThat(comment.deletedAt).isNull()
    }

    @Test
    fun `목록은 원댓글만 페이징하고 답글을 replies 에 담는다`() {

        val member = buildMember(1L)
        val post = buildPost(10L)
        val root = buildComment(id = 50L, post = post, parent = null, author = member)
        val deletedRoot = buildComment(id = 60L, post = post, parent = null, author = member)
        val reply = buildComment(id = 100L, post = post, parent = root, author = member)
        val replyOfDeleted = buildComment(id = 110L, post = post, parent = deletedRoot, author = member)

        deletedRoot.softDelete(LocalDateTime.now())

        val pageable = PageRequest.of(
            0,
            20
        )

        whenever(communityAuthorizationService.getActivePost(10L)).thenReturn(post)
        whenever(communityCommentRepository.findRootCommentsByPostId(any(), any())).thenReturn(
            PageImpl(
                listOf(
                    root,
                    deletedRoot
                ),
                pageable,
                2L
            )
        )
        whenever(communityCommentRepository.findRepliesByParentIds(any())).thenReturn(
            listOf(
                reply,
                replyOfDeleted
            )
        )
        whenever(profileService.getImageUrlsByMemberIds(any())).thenReturn(emptyMap())

        val result = service.getComments(
            10L,
            pageable
        )

        assertThat(result.totalElements).isEqualTo(2L)
        assertThat(result.content.map { comment -> comment.id }).containsExactly(50L, 60L)
        assertThat(result.content[0].replies.map { comment -> comment.id }).containsExactly(100L)
        assertThat(result.content[0].replies[0].parentId).isEqualTo(50L)
        assertThat(result.content[1].isDeleted).isTrue()
        assertThat(result.content[1].content).isEqualTo("삭제된 댓글입니다")
        assertThat(result.content[1].replies.map { comment -> comment.id }).containsExactly(110L)
    }

    private fun buildMember(
        id: Long
    ): Member {

        val member: Member = mock()
        whenever(member.getId()).thenReturn(id)
        whenever(member.getDisplayUsername()).thenReturn("user$id")
        whenever(member.isDeleted()).thenReturn(false)

        return member
    }

    private fun buildPost(
        id: Long
    ): CommunityPost {

        val post: CommunityPost = mock()
        whenever(post.getId()).thenReturn(id)

        return post
    }

    private fun buildComment(
        id: Long,
        post: CommunityPost,
        parent: CommunityComment?,
        author: Member
    ): CommunityComment {

        return CommunityComment(
            member = author,
            communityPost = post,
            content = "c$id",
            isEdited = false,
            deletedAt = null,
            parent = parent
        ).also { comment ->

            val idField = CommunityComment::class.java.getDeclaredField("id")
            idField.isAccessible = true
            idField.set(comment, id)
        }
    }
}
