package spring.springserver.domain.location.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "sig")
class Sig(

    @Id
    @Column(name = "sig_cd", length = 5)
    private val sigCd: String,

    @Column(name = "sig_eng_nm", length = 40)
    val sigEngNm: String?,

    @Column(name = "sig_kor_nm", length = 40)
    val sigKorNm: String?
) {

    /**
     * 시군구 중심점의 위도. 거리 기반 필터·알림에만 쓰이며 시드로 채운다.
     * 좌표가 아직 없는 시군구가 있을 수 있으므로 nullable이다.
     */
    @Column(name = "latitude")
    private var latitude: Double? = null

    @Column(name = "longitude")
    private var longitude: Double? = null

    fun getSigCd(): String = sigCd

    fun getCtprvnCd(): String = sigCd.substring(0, 2)

    fun getLatitude(): Double? = latitude
    fun getLongitude(): Double? = longitude

    fun hasCoordinate(): Boolean = latitude != null && longitude != null

    fun updateCoordinate(
        latitude: Double,
        longitude: Double
    ) {

        this.latitude = latitude
        this.longitude = longitude
    }
}
