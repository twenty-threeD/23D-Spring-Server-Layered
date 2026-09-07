package spring.springserver.domain.contract.repository

import org.springframework.data.jpa.repository.JpaRepository
import spring.springserver.domain.contract.entity.Contract

interface ContractRepository: JpaRepository<Contract, Long> {

    fun findAllByContractUrl(
        contractUrl: String
    ): List<Contract>
}