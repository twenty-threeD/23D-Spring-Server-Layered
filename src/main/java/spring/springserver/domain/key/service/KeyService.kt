package spring.springserver.domain.key.service

import spring.springserver.domain.member.entity.Member

interface KeyService {

    fun generateKeyPair(memberId: Long)
}