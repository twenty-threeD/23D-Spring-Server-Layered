package spring.springserver.domain.contract.entity

/**
 * 계약서의 서명 진행 상태.
 * 전문가가 계약서를 등록한 시점에는 DRAFT이고,
 * 의뢰인과 전문가가 모두 서명하면 SIGNED가 된다.
 */
enum class ContractStatus {

    DRAFT,
    SIGNED
}
