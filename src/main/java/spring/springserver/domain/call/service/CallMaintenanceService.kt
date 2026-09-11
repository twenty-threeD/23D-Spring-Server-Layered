package spring.springserver.domain.call.service

/**
 * 스케줄러가 호출하는 통화 정리 작업.
 */
interface CallMaintenanceService {

    /**
     * 응답이 없는 통화는 부재중으로, 너무 오래 연결돼 있는 통화는 종료로 정리한다.
     */
    fun expireStaleCalls()
}
