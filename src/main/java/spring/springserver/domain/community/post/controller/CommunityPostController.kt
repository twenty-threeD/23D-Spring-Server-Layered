package spring.springserver.domain.community.post.controller

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.validation.ConstraintViolationException
import jakarta.validation.Valid
import jakarta.validation.Validator
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import spring.springserver.domain.community.common.data.response.DeleteResponse
import spring.springserver.domain.community.post.data.request.CreatePostRequest
import spring.springserver.domain.community.post.data.request.UpdatePostRequest
import spring.springserver.domain.community.post.data.response.CommunityPostResponse
import spring.springserver.domain.community.post.data.response.CreatePostResponse
import spring.springserver.domain.community.post.data.response.UpdatePostResponse
import spring.springserver.domain.community.post.service.CommunityPostService
import spring.springserver.global.data.BaseResponse
import spring.springserver.global.exception.exception.ApplicationException
import spring.springserver.global.exception.status_code.CommonStatusCode

@RestController
@RequestMapping("/api/community/post")
class CommunityPostController(
    private val communityPostService: CommunityPostService,
    private val objectMapper: ObjectMapper,
    private val validator: Validator
) {

    @PostMapping(consumes = [MediaType.APPLICATION_JSON_VALUE])
    fun createPost(
        @Valid @RequestBody createPostRequest: CreatePostRequest
    ): BaseResponse<CreatePostResponse> {

        return BaseResponse.ok(communityPostService.createPost(createPostRequest))
    }

    @PostMapping(consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun createPostWithFile(
        @RequestPart("createPostRequest") createPostRequestJson: String,
        @RequestPart(value = "file", required = false) file: MultipartFile?
    ): BaseResponse<CreatePostResponse> {

        val createPostRequest = parseCreatePostRequest(createPostRequestJson)

        return BaseResponse.ok(communityPostService.createPost(createPostRequest, file))
    }

    @PatchMapping(consumes = [MediaType.APPLICATION_JSON_VALUE])
    fun updatePost(
        @Valid @RequestBody updatePostRequest: UpdatePostRequest
    ): BaseResponse<UpdatePostResponse> {

        return BaseResponse.ok(communityPostService.updatePost(updatePostRequest))
    }

    @PatchMapping(consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun updatePostWithFile(
        @RequestPart("updatePostRequest") updatePostRequestJson: String,
        @RequestPart(value = "file", required = false) file: MultipartFile?
    ): BaseResponse<UpdatePostResponse> {

        val updatePostRequest = parseUpdatePostRequest(updatePostRequestJson)

        return BaseResponse.ok(communityPostService.updatePost(updatePostRequest, file))
    }

    @DeleteMapping
    fun deletePost(
        @RequestParam postId: Long
    ): BaseResponse<DeleteResponse> {

        return BaseResponse.ok(communityPostService.deletePost(postId))
    }

    @GetMapping
    fun getPosts(): BaseResponse<List<CommunityPostResponse>> {

        return BaseResponse.ok(communityPostService.getPosts())
    }

    @GetMapping("/{postId}")
    fun getPost(
        @PathVariable postId: Long
    ): BaseResponse<CommunityPostResponse> {

        return BaseResponse.ok(communityPostService.getPost(postId))
    }

    @GetMapping("/search")
    fun searchPosts(
        @RequestParam keyword: String
    ): BaseResponse<List<CommunityPostResponse>> {

        return BaseResponse.ok(communityPostService.searchPosts(keyword))
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
