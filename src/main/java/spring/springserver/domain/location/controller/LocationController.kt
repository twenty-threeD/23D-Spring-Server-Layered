package spring.springserver.domain.location.controller

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import spring.springserver.domain.location.data.response.SidoResponse
import spring.springserver.domain.location.data.response.SigunguResponse
import spring.springserver.domain.location.service.LocationService
import spring.springserver.global.data.BaseResponse

@RestController
@RequestMapping("/api/location")
class LocationController(
    private val locationService: LocationService
) {

    @GetMapping("/sido")
    fun getSidoList(): BaseResponse<List<SidoResponse>> {

        return BaseResponse.ok(locationService.getSidoList())
    }

    @GetMapping("/sigungu")
    fun getSigunguList(@RequestParam ctprvnCd: String): BaseResponse<List<SigunguResponse>> {

        return BaseResponse.ok(locationService.getSigunguList(ctprvnCd))
    }
}