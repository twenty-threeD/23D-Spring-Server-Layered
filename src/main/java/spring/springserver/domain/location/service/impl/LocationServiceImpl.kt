package spring.springserver.domain.location.service.impl

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import spring.springserver.domain.location.data.response.SidoResponse
import spring.springserver.domain.location.data.response.SigunguResponse
import spring.springserver.domain.location.entity.Sig
import spring.springserver.domain.location.exception.LocationStatusCode
import spring.springserver.domain.location.repository.CtprvnRepository
import spring.springserver.domain.location.repository.SigRepository
import spring.springserver.domain.location.service.LocationService
import spring.springserver.global.exception.exception.ApplicationException
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

@Service
@Transactional(readOnly = true)
class LocationServiceImpl(
    private val sigRepository: SigRepository,
    private val ctprvnRepository: CtprvnRepository
): LocationService {

    override fun getSidoList(): List<SidoResponse> {

        return ctprvnRepository.findAllOrderByCtprvnCd()
            .map {
                ctprvn -> SidoResponse.of(ctprvn)
            }
    }

    override fun getSigunguList(
        ctprvnCd: String
    ): List<SigunguResponse> {

        if (!ctprvnRepository.existsById(ctprvnCd)) {

            throw ApplicationException(LocationStatusCode.SIDO_NOT_FOUND)
        }

        return sigRepository.findAllByCtprvnCd(ctprvnCd)
            .map {
                sig -> SigunguResponse.of(sig)
            }
    }

    override fun getSig(
        sigCd: String
    ): Sig {

        return sigRepository.findById(sigCd).orElse(null)
            ?: throw ApplicationException(LocationStatusCode.SIGUNGU_NOT_FOUND)
    }

    override fun getFullName(
        sig: Sig
    ): String {

        val ctprvn = ctprvnRepository.findById(sig.getCtprvnCd()).orElse(null)
            ?: throw ApplicationException(LocationStatusCode.SIDO_NOT_FOUND)

        return "${ctprvn.ctpKorNm ?: ""} ${sig.sigKorNm ?: ""}".trim()
    }

    override fun findNearbySigCds(
        sigCd: String,
        radiusKm: Double
    ): List<String> {

        val origin = getSig(sigCd)

        val originLatitude = origin.getLatitude()
        val originLongitude = origin.getLongitude()

        if (originLatitude == null || originLongitude == null) {

            return listOf(origin.getSigCd())
        }

        val nearbySigCds = sigRepository.findAllWithCoordinate()
            .filter {

                sig ->
                distanceKm(
                    originLatitude,
                    originLongitude,
                    sig.getLatitude()!!,
                    sig.getLongitude()!!
                ) <= radiusKm
            }
            .map { sig -> sig.getSigCd() }

        return nearbySigCds.ifEmpty { listOf(origin.getSigCd()) }
    }

    /**
     * 두 좌표 사이의 대권 거리(km). 시군구 중심점끼리의 근사 비교에만 쓰므로
     * 지구를 완전한 구로 보는 하버사인 공식으로 충분하다.
     */
    private fun distanceKm(
        originLatitude: Double,
        originLongitude: Double,
        targetLatitude: Double,
        targetLongitude: Double
    ): Double {

        val latitudeDelta = Math.toRadians(targetLatitude - originLatitude)
        val longitudeDelta = Math.toRadians(targetLongitude - originLongitude)

        val haversine = sin(latitudeDelta / 2).pow(2) +
            cos(Math.toRadians(originLatitude)) *
            cos(Math.toRadians(targetLatitude)) *
            sin(longitudeDelta / 2).pow(2)

        return 2 * EARTH_RADIUS_KM * asin(min(1.0, sqrt(haversine)))
    }

    companion object {

        private const val EARTH_RADIUS_KM = 6371.0
    }
}
