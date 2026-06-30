package spring.springserver.domain.location.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "ctprvn")
class Ctprvn(

    @Id
    @Column(name = "ctprvn_cd", length = 2)
    private val ctprvnCd: String,

    @Column(name = "ctp_eng_nm", length = 40)
    val ctpEngNm: String?,

    @Column(name = "ctp_kor_nm", length = 40)
    val ctpKorNm: String?
) {

    fun getCtprvnCd(): String = ctprvnCd
}