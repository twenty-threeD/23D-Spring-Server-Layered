package spring.springserver.domain.location.initializer

import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import spring.springserver.domain.location.repository.SigRepository

/**
 * 시군구 중심좌표를 CSV에서 읽어 sig 테이블에 채운다.
 *
 * resources/sig-coordinate.csv 형식(첫 줄은 헤더):
 *   sig_cd,latitude,longitude
 *   11110,37.5834,126.9790
 *
 * 파일이 없으면 아무것도 하지 않는다. 좌표가 비어 있으면 거리 기반 필터와 알림이
 * 기준 시군구 하나로만 좁혀져 동작하므로, 기능이 죽지는 않고 범위만 줄어든다.
 */
@Component
class SigCoordinateSeedRunner(
    private val sigRepository: SigRepository
): ApplicationRunner {

    companion object {

        private val log = LoggerFactory.getLogger(SigCoordinateSeedRunner::class.java)

        private const val RESOURCE_PATH = "sig-coordinate.csv"

        private const val COLUMN_COUNT = 3
    }

    @Transactional(rollbackFor = [Exception::class])
    override fun run(applicationArguments: ApplicationArguments) {

        val resource = ClassPathResource(RESOURCE_PATH)

        if (!resource.exists()) {

            log.info("Sig coordinate seed skipped. (resource not found: {})", RESOURCE_PATH)

            return
        }

        val coordinates = readCoordinates(resource)

        if (coordinates.isEmpty()) {

            return
        }

        /**
         * 이미 좌표가 있는 시군구는 건드리지 않는다.
         * 좌표를 다시 채우고 싶으면 해당 행의 latitude/longitude를 비우고 재기동한다.
         */
        val updated = sigRepository.findAllById(coordinates.keys)
            .filterNot { sig -> sig.hasCoordinate() }
            .onEach {

                sig ->
                val coordinate = coordinates.getValue(sig.getSigCd())

                sig.updateCoordinate(coordinate.first, coordinate.second)
            }

        if (updated.isNotEmpty()) {

            log.info("Seeded sig coordinates. (updated: {})", updated.size)
        }
    }

    private fun readCoordinates(
        resource: ClassPathResource
    ): Map<String, Pair<Double, Double>> {

        val coordinates = mutableMapOf<String, Pair<Double, Double>>()

        resource.inputStream.bufferedReader().useLines {

            lines ->
            lines.drop(1)
                .forEach { line -> parseLine(line)?.let { coordinates[it.first] = it.second } }
        }

        return coordinates
    }

    /**
     * 형식이 어긋난 줄은 건너뛴다. 한 줄 때문에 애플리케이션 기동이 막히면 안 된다.
     */
    private fun parseLine(
        line: String
    ): Pair<String, Pair<Double, Double>>? {

        val columns = line.trim()
            .takeIf { it.isNotBlank() }
            ?.split(",")
            ?: return null

        if (columns.size < COLUMN_COUNT) {

            log.warn("Skipped malformed sig coordinate line. (line: {})", line)

            return null
        }

        val sigCd = columns[0].trim()
        val latitude = columns[1].trim().toDoubleOrNull()
        val longitude = columns[2].trim().toDoubleOrNull()

        if (sigCd.isBlank() || latitude == null || longitude == null) {

            log.warn("Skipped malformed sig coordinate line. (line: {})", line)

            return null
        }

        return sigCd to (latitude to longitude)
    }
}
