package spring.springserver.support

/**
 * 서버 코드가 직접 부르는 PostgreSQL 함수를 H2에서 같은 이름으로 쓸 수 있게 하는 대체 함수.
 * application-test.yaml의 datasource URL INIT에서 CREATE ALIAS로 등록한다.
 * 테스트는 요청을 하나씩 보내므로 잠금은 아무것도 하지 않는다.
 */
object H2Functions {

    @JvmStatic
    fun pgAdvisoryXactLock(
        key: Long
    ) {

    }

    @JvmStatic
    fun pgAdvisoryXactLock(
        key1: Int,
        key2: Int
    ) {

    }

    @JvmStatic
    fun hashtext(
        text: String
    ): Int {

        return text.hashCode()
    }
}