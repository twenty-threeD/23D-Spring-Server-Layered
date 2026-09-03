package spring.springserver.domain.community.job.data.request

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import spring.springserver.domain.community.job.entity.JobPostType

data class UpdateJobPostRequest(
    @field:NotNull(message = "게시글 아이디는 필수입니다.")
    val postId: Long?,

    @field:NotNull(message = "구인/구직 구분은 필수입니다.")
    val postType: JobPostType?,

    @field:NotBlank(message = "제목은 필수입니다.")
    @field:Size(max = 255, message = "제목은 255자 이하로 입력해주세요.")
    val title: String?,

    @field:Size(max = 2000, message = "내용은 2000자 이하로 입력해주세요.")
    val content: String?,

    @field:Size(max = 2000, message = "첨부 파일 경로는 2000자 이하로 입력해주세요.")
    val fileUrl: String?,

    @field:NotNull(message = "용역 카테고리는 필수입니다.")
    val jobCategoryId: Long?,

    @field:NotBlank(message = "지역은 필수입니다.")
    val sigCd: String?
)
