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

@Service
@Transactional(readOnly = true)
class LocationServiceImpl(
    private val sigRepository: SigRepository,
    private val ctprvnRepository: CtprvnRepository
): LocationService {

    override fun getSidoList(): List<SidoResponse> {

        return ctprvnRepository.findAllOrderByCtprvnCd()
            .map { ctprvn -> SidoResponse.of(ctprvn) }
    }

    override fun getSigunguList(
        ctprvnCd: String
    ): List<SigunguResponse> {

        if (!ctprvnRepository.existsById(ctprvnCd)) {

            throw ApplicationException(LocationStatusCode.SIDO_NOT_FOUND)
        }

        return sigRepository.findAllByCtprvnCd(ctprvnCd)
            .map { sig -> SigunguResponse.of(sig) }
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
}