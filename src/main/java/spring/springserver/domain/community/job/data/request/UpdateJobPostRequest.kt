package spring.springserver.domain.community.job.data.request

import com.fasterxml.jackson.annotation.JsonAlias
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import spring.springserver.domain.community.job.entity.JobPostType

data class UpdateJobPostRequest(
    /**
     * 프론트가 id로 보내므로 두 이름을 모두 받는다.
     */
    @field:NotNull(message = "게시글 아이디는 필수입니다.")
    @field:JsonAlias("id")
    val postId: Long?,

    /**
     * 비우면 기존 값을 유지한다.
     */
    val postType: JobPostType? = null,

    @field:NotBlank(message = "제목은 필수입니다.")
    @field:Size(max = 255, message = "제목은 255자 이하로 입력해주세요.")
    val title: String?,

    @field:NotBlank(message = "내용은 필수입니다.")
    @field:Size(max = 2000, message = "내용은 2000자 이하로 입력해주세요.")
    val content: String?,

    @field:Size(max = 2000, message = "첨부 파일 경로는 2000자 이하로 입력해주세요.")
    val fileUrl: String?,

    @field:NotNull(message = "용역 카테고리는 필수입니다.")
    @field:JsonAlias("categoryId")
    val jobCategoryId: Long?,

    /**
     * 비우면 기존 값을 유지한다.
     */
    val sigCd: String? = null
)
