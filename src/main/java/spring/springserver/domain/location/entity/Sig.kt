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
    val sigCd: String,

    @Column(name = "sig_eng_nm", length = 40)
    val sigEngNm: String?,

    @Column(name = "sig_kor_nm", length = 40)
    val sigKorNm: String?
) {

    fun getCtprvnCd(): String = sigCd.substring(0, 2)
}