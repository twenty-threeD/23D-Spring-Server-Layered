package spring.springserver.domain.community.job.data.request

import com.fasterxml.jackson.annotation.JsonAlias
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import spring.springserver.domain.community.job.entity.JobPostType

data class CreateJobPostRequest(
    /**
     * HIRING(구인) 또는 SEEKING(구직). 비우면 HIRING으로 본다.
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

    /**
     * 프론트가 categoryId로 보내므로 두 이름을 모두 받는다.
     */
    @field:NotNull(message = "용역 카테고리는 필수입니다.")
    @field:JsonAlias("categoryId")
    val jobCategoryId: Long?,

    /**
     * 시군구 행정코드 5자리. 거리 기반 필터와 알림의 기준점이 된다.
     * 비우면 작성자 프로필에 설정된 지역을 쓴다.
     */
    val sigCd: String? = null
)
