package spring.springserver.domain.file.initializer

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

/**
 * 프로필 이미지, 커뮤니티 글 파일, 게시글 첨부 URL은 요청 검증으로 업로드 경로(/files/<파일명>)만 받는다.
 * 검증이 생기기 전에 저장된 외부 URL은 그대로 남아 있으므로, 설정을 켰을 때 기동 시 한 번 비운다.
 *
 * 비운 프로필 이미지는 기본 이미지로, 비운 글 파일과 게시글 첨부는 첨부 없음으로 보인다.
 * 요청 검증과 같은 규칙을 쓰고, 정리한 뒤에는 대상이 없어 다시 켜도 안전하다.
 */
@Component
class ExternalFileUrlCleanupRunner(
    private val jdbcTemplate: JdbcTemplate,
    @param:Value($$"${app.upload.cleanup-external-urls:false}") private val enabled: Boolean
) : ApplicationRunner {

    companion object {

        private val log = LoggerFactory.getLogger(ExternalFileUrlCleanupRunner::class.java)

        private const val UPLOADED_FILE_URL_PATTERN = "^\\s*(/files/[^/\\s.][^/\\s]*)?\\s*$"
    }

    @Transactional(rollbackFor = [Exception::class])
    override fun run(
        args: ApplicationArguments
    ) {

        if (!enabled) {

            return
        }

        val profileCount = clearExternalUrls(
            "profile",
            "image_url"
        )
        val communityPostCount = clearExternalUrls(
            "community_post",
            "file_url"
        )
        val postAttachCount = clearExternalUrls(
            "post_attach",
            "file_url"
        )

        log.info(
            "외부 파일 URL을 정리했습니다. profile.image_url = {}, community_post.file_url = {}, post_attach.file_url = {}",
            profileCount,
            communityPostCount,
            postAttachCount
        )
    }

    private fun clearExternalUrls(
        table: String,
        column: String
    ): Int {

        if (jdbcTemplate.queryForObject("select to_regclass('$table') is not null", Boolean::class.java) != true) {

            return 0
        }

        return jdbcTemplate.update(
            "update $table set $column = null where $column is not null and $column !~ ?",
            UPLOADED_FILE_URL_PATTERN
        )
    }
}
