package spring.springserver.domain.community.post.service.impl

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.validation.ConstraintViolationException
import jakarta.validation.Validator
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import spring.springserver.domain.community.comment.repository.CommunityCommentRepository
import spring.springserver.domain.community.common.data.response.DeleteResponse
import spring.springserver.domain.community.common.service.CommunityAuthorizationService
import spring.springserver.domain.community.like.repository.CommunityPostLikeRepository
import spring.springserver.domain.community.post.data.request.CreatePostRequest
import spring.springserver.domain.community.post.data.request.UpdatePostRequest
import spring.springserver.domain.community.post.data.response.CommunityPostResponse
import spring.springserver.domain.community.post.data.response.CreatePostResponse
import spring.springserver.domain.community.post.data.response.UpdatePostResponse
import spring.springserver.domain.community.post.repository.CommunityPostRepository
import spring.springserver.domain.community.post.service.CommunityPostService
import spring.springserver.domain.file.data.request.FileUploadRequest
import spring.springserver.domain.file.service.FileService
import spring.springserver.global.exception.exception.ApplicationException
import spring.springserver.global.exception.status_code.CommonStatusCode
import java.time.LocalDateTime

@Service
@Transactional(rollbackFor = [Exception::class])
class CommunityPostServiceImpl(
    private val communityPostRepository: CommunityPostRepository,
    private val communityCommentRepository: CommunityCommentRepository,
    private val communityPostLikeRepository: CommunityPostLikeRepository,
    private val communityAuthorizationService: CommunityAuthorizationService,
    private val fileService: FileService,
    private val objectMapper: ObjectMapper,
    private val validator: Validator
): CommunityPostService {

    override fun createPost(
        createPostRequest: CreatePostRequest,
        file: MultipartFile?
    ): CreatePostResponse {

        val member = communityAuthorizationService.getCurrentMember()
        val uploadedFileUrl = uploadFile(file)

        val communityPost = communityPostRepository.save(
            createPostRequest.toEntity(member, uploadedFileUrl)
        )

        return CreatePostResponse.of(communityPost)
    }

    override fun createPost(
        createPostRequestJson: String,
        file: MultipartFile?
    ): CreatePostResponse {

        return createPost(
            parseCreatePostRequest(createPostRequestJson),
            file
        )
    }

    override fun updatePost(
        updatePostRequest: UpdatePostRequest,
        file: MultipartFile?
    ): UpdatePostResponse {

        val member = communityAuthorizationService.getCurrentMember()

        val communityPost = communityAuthorizationService
            .getActivePost(updatePostRequest.postId)

        communityAuthorizationService.validateOwner(member, communityPost.member.getId())
        val uploadedFileUrl = uploadFile(file)

        communityPost.update(
            title = updatePostRequest.title.trim(),
            content = updatePostRequest.content?.trim()?.takeIf { it.isNotBlank() },
            fileUrl = uploadedFileUrl ?: updatePostRequest.fileUrl?.trim()?.takeIf { it.isNotBlank() },
        )

        return UpdatePostResponse.of(communityPost)
    }

    override fun updatePost(
        updatePostRequestJson: String,
        file: MultipartFile?
    ): UpdatePostResponse {

        return updatePost(
            parseUpdatePostRequest(updatePostRequestJson),
            file
        )
    }

    override fun deletePost(
        postId: Long
    ): DeleteResponse {

        val member = communityAuthorizationService.getCurrentMember()

        val communityPost = communityAuthorizationService.getActivePost(postId)

        communityAuthorizationService.validateOwner(
            member,
            communityPost.member.getId()
        )

        communityPost.softDelete(LocalDateTime.now())

        return DeleteResponse.of("삭제되었습니다.")
    }

    @Transactional(readOnly = true)
    override fun getPosts(): List<CommunityPostResponse> {

        return communityPostRepository.findAllByDeletedAtIsNullOrderByUpdatedAtDesc()
            .map {
                communityPost ->
                CommunityPostResponse.toPostResponse(
                    communityPost,
                    communityCommentRepository,
                    communityPostLikeRepository
                )
            }
    }

    @Transactional(readOnly = true)
    override fun getPost(
        postId: Long
    ): CommunityPostResponse {

        val communityPost = communityAuthorizationService.getActivePost(postId)

        communityPost.increaseViewCount()

        return CommunityPostResponse.toPostResponse(
            communityPost,
            communityCommentRepository,
            communityPostLikeRepository
        )
    }

    @Transactional(readOnly = true)
    override fun searchPosts(
        keyword: String
    ): List<CommunityPostResponse> {

        val normalizedKeyword = keyword.trim()

        return communityPostRepository.searchPosts(normalizedKeyword)
            .map {
                communityPost ->
                CommunityPostResponse.toPostResponse(
                    communityPost,
                    communityCommentRepository,
                    communityPostLikeRepository
                )
            }
    }

    private fun uploadFile(
        file: MultipartFile?
    ): String? {

        return file
            ?.takeIf { !it.isEmpty }
            ?.let { fileService.uploadFile(FileUploadRequest(it)).fileUrl() }
    }

    private fun parseCreatePostRequest(
        rawJson: String
    ): CreatePostRequest {

        val jsonNode = readJsonPart(rawJson, "createPostRequest")

        return validate(
            CreatePostRequest(
                title = requiredText(jsonNode, "title"),
                content = optionalText(jsonNode, "content"),
                fileUrl = optionalText(jsonNode, "fileUrl"),
            )
        )
    }

    private fun parseUpdatePostRequest(
        rawJson: String
    ): UpdatePostRequest {

        val jsonNode = readJsonPart(rawJson, "updatePostRequest")

        return validate(
            UpdatePostRequest(
                postId = requiredLong(jsonNode, "postId"),
                title = requiredText(jsonNode, "title"),
                content = optionalText(jsonNode, "content"),
                fileUrl = optionalText(jsonNode, "fileUrl"),
            )
        )
    }

    private fun readJsonPart(
        rawJson: String,
        partName: String
    ): JsonNode {

        return try {

            objectMapper.readTree(rawJson)
        } catch (exception: Exception) {

            throw ApplicationException(
                CommonStatusCode.INVALID_ARGUMENT,
                "$partName JSON 형식이 올바르지 않습니다."
            )
        }
    }

    private fun requiredText(
        jsonNode: JsonNode,
        fieldName: String
    ): String {

        return optionalText(jsonNode, fieldName) ?: ""
    }

    private fun optionalText(
        jsonNode: JsonNode,
        fieldName: String
    ): String? {

        val field = jsonNode.get(fieldName)

        return if (field == null || field.isNull) {

            null
        } else {

            field.asText()
        }
    }

    private fun requiredLong(
        jsonNode: JsonNode,
        fieldName: String
    ): Long {

        val field = jsonNode.get(fieldName)
        val value = if (field == null || field.isNull) {

            null
        } else {

            field.asText().toLongOrNull()
        }

        return value ?: throw ApplicationException(
            CommonStatusCode.INVALID_ARGUMENT,
            "$fieldName 값이 올바르지 않습니다."
        )
    }

    private fun <T : Any> validate(
        request: T
    ): T {

        val violations = validator.validate(request)

        if (violations.isNotEmpty()) {
            
            throw ConstraintViolationException(violations)
        }

        return request
    }
}
