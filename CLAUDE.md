# 23D Spring Server (Layered)

Kotlin + Spring Boot 3.5 / Java 21 툴체인 / Gradle(Groovy DSL) 기반 REST API 서버.

## 절대 규칙

- **모든 답변은 한국어로 한다.** 코드·식별자·주석은 영어를 써도 되지만 설명·요약·리뷰 결과는 한국어.
- **코드를 쓰거나 고치기 전에 반드시 `.claude/convention.md`를 읽는다.** 이 프로젝트의 컨벤션은 일반적인 Kotlin 스타일과 다른 부분이 많다(여는 중괄호 뒤 빈 줄, `return` 앞 빈 줄, 파라미터 줄바꿈 등). 컨벤션과 요청이 충돌하면 먼저 알리고, 사용자가 명시적으로 뒤집지 않는 한 컨벤션을 따른다.
- `.env`는 실제 운영 비밀값이다. 읽지 말고, 값을 코드·로그·문서에 옮기지 않는다.
- 요청받지 않은 리팩토링·의존성 추가·파일 생성을 하지 않는다.

## 소스 레이아웃

주의: **소스는 Kotlin인데 디렉터리는 `src/main/java`**다. 새 파일도 이 경로 아래에 만든다.

```
src/main/java/spring/springserver/
├── domain/<도메인>/
│   ├── controller/          @RestController, "/api/<도메인>"
│   ├── service/             인터페이스(UseCase) + impl/ 구현체
│   ├── repository/          Spring Data 인터페이스
│   ├── entity/              JPA 엔티티
│   ├── data/request/        요청 DTO (Bean Validation)
│   ├── data/response/       응답 DTO (companion object의 of() 팩토리)
│   └── exception/           <도메인>StatusCode enum
└── global/
    ├── config/              Security, WebSocket, Redis, Swagger, Mail …
    ├── jwt/                 JwtProvider, JwtAuthFilter, MemberDetails
    ├── handler/             GlobalExceptionHandler(@RestControllerAdvice)
    ├── data/                BaseResponse, ErrorResponse
    └── exception/           ApplicationException, StatusCode, CommonStatusCode
```

도메인: auth, member, profile, post, community, chat, notification, payment,
file, email, location, jobcategory, key, blockchain

## 아키텍처 계약

- **계층**: Controller → Service(인터페이스) → ServiceImpl → Repository. 컨트롤러에 비즈니스 로직 금지, 컨트롤러에서 Repository 직접 호출 금지.
- **서비스는 항상 인터페이스 먼저** 만들고 `impl/` 아래 구현체를 둔다. 의존성은 생성자 주입만 사용(필드 주입·`@Autowired` 금지).
- **응답은 항상 `BaseResponse<T>`**로 감싼다. 컨트롤러 반환 타입은 `BaseResponse<XxxResponse>`.
- **에러는 항상 `ApplicationException(XxxStatusCode.YYY)`**로 던진다. 컨트롤러에서 try/catch 하지 않고 `GlobalExceptionHandler`가 처리한다. 새 에러가 필요하면 해당 도메인의 `StatusCode` enum에 항목을 추가한다(코드/메시지/HttpStatus).
- **엔티티 ID**는 `private var id: Long? = null` + `fun getId() = id`. setter를 만들지 않는다.
- **Optional/null**은 `?: throw ApplicationException(...)` 형태로 처리한다.
- 클래스 생성은 Builder 없이 생성자로 한다.
- `@Transactional(rollbackFor = [Exception::class])`를 서비스 구현체에 붙이고, 조회 전용 메서드는 `@Transactional(readOnly = true)`.

## 인증/인가

JWT(jjwt) + Spring Security. 새 엔드포인트를 추가하면 **`global/config/SecurityConfig.kt`의 `authorizeHttpRequests` 규칙도 반드시 같이 갱신**한다. 규칙을 빠뜨리면 `anyRequest().authenticated()`에 걸린다. 공개 API가 아니면 `permitAll()`을 쓰지 않는다.

## 빌드 / 실행

```bash
./gradlew build -x test     # 컴파일 확인 (가장 자주 쓰는 검증 수단)
./gradlew bootRun           # 로컬 실행 (.env 필요)
```

- 테스트 하네스는 아직 도입하지 않았다(`src/test` 없음). **요청 없이 테스트 코드를 만들지 않는다.**
- 변경 후 검증은 `./gradlew build -x test` 컴파일 통과로 한다.
- 인프라: PostgreSQL, Redis, MongoDB. 설정은 `src/main/resources/application.yaml`, 값은 `.env`에서 주입.

## 작업 흐름

1. 관련 도메인의 기존 파일을 먼저 읽어 패턴을 파악한다(가장 가까운 유사 도메인을 복제하듯 따른다).
2. 구현한다.
3. `./gradlew build -x test`로 컴파일을 확인한다.
4. 변경 파일 목록과 남은 리스크를 한국어로 요약한다.

## Git

- 기본 브랜치 `main`, 작업 브랜치 `develop` 및 `<이슈번호>-<타입>-<설명>` (예: `130-fix-merge-oauth-package-with-auth`).
- 커밋 메시지: `feat: `, `fix: `, `refactor: ` 등 + 한국어 설명.
- 커밋·푸시는 사용자가 요청할 때만 한다.
