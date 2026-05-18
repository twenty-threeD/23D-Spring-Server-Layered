package spring.springserver.domain.community.shared.data.response

data class DeleteResponse(
    val message: String
) {
    companion object {

        fun ok(): DeleteResponse {

            return DeleteResponse("삭제되었습니다.")
        }
    }
}
