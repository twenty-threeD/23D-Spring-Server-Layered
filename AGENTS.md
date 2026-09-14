# AGENTS.md

This file provides guidance to Claude Code(claude.ai/code) & Codex when working with code in this repository.
루트 `CLAUDE.md`는 이 파일을 `@AGENTS.md`로 가져오기만 하는 포인터다. 지침을 고칠 때는 이 파일만 고친다.

# 23D Spring Server (Layered)

Kotlin + Spring Boot 3.5 / Java 21 툴체인 / Gradle(Kotlin DSL, `build.gradle.kts`) 기반 REST API 서버.

## 절대 규칙
- 파일은 사용자의 승인 없이 수정/생성 하지 않는다. 단, 조회는 승인없이 진행해도 된다.
- **모든 답변은 한국어로 한다.** 코드·식별자·주석은 영어를 써도 되지만 설명·요약·리뷰 결과는 한국어.
- **코드를 쓰거나 고치기 전에 반드시 `.agents/convention.md`를 읽는다.** 이 파일이 컨벤션의 단일 출처다(루트 `CONVENTIONS.md`는 본문 없이 이 파일을 가리키는 리뷰용 프롬프트일 뿐이다). 이 프로젝트의 컨벤션은 일반적인 Kotlin 스타일과 다른 부분이 많다(여는 중괄호 뒤 빈 줄, `return` 앞 빈 줄, 파라미터 줄바꿈 등). 컨벤션과 요청이 충돌하면 먼저 알리고, 사용자가 명시적으로 뒤집지 않는 한 컨벤션을 따른다.
- `.env`는 실제 운영 비밀값입니다. 읽지 않고, 값을 코드·로그·문서에 옮기지 않는 것을 원칙으로 하나 사용자의 요청에 따라 값을 조회할 수 있다. 단, 사용자의 요청에 대해서도 2회 이상 재차 확인한다.
- 요청받지 않은 리팩토링·의존성 추가·파일 생성을 하지 않는다.

## 소스 레이아웃
주의: **소스는 Kotlin인데 디렉터리는 `src/main/java`**다. 새 파일도 이 경로 아래에 만든다.

```
src/main/java/spring/springserver/
├── domain/<도메인>/
│   ├── controller/          @RestController, "/api/<도메인>"
│   ├── service/             인터페이스(UseCase) + impl/ 구현체
│   ├── repository/          Spring Data 인터페이스
│   ├── entity/              JPA 엔티티 + 상태 enum
│   ├── data/request/        요청 DTO (Bean Validation)
│   ├── data/response/       응답 DTO (companion object의 of() 팩토리)
│   └── exception/           <도메인>StatusCode enum
└── global/
    ├── config/              SecurityConfig, WebConfig, TossPaymentsProperties,
    │                        AgoraProperties, CookieOAuth2AuthorizationRequestRepository,
    │                        하위 폴더 redis/ mail/ swagger/ websocket/ blockchain/
    ├── jwt/                 JwtProvider, TokenProvider, JwtAuthFilter, MemberDetails(Service)
    ├── handler/             GlobalExceptionHandler(@RestControllerAdvice), 인증/인가 핸들러
    ├── data/                BaseResponse, ErrorResponse
    ├── exception/exception/ ApplicationException
    ├── exception/status_code/ StatusCode 인터페이스, CommonStatusCode
    └── util/
```

도메인: auth, member, profile, post, community, chat, call, notification, payment,
contract, estimate, file, email, phone, location, jobcategory, key, blockchain

`community`만 예외로 한 단계 더 중첩된다: `community/{post,comment,like,job,common}` 각각이
위의 6종 구조를 따로 갖는다. 나머지 도메인은 모두 평평한 6종 구조다.

필요한 도메인은 위 6종 외 디렉터리를 더 쓴다. 실제로 쓰이는 것:
`client/`(payment), `scheduler/`(auth, call, payment), `event/`(call, chat, community/job),
`listener/`(notification), `handler/`(auth), `initializer/`(jobcategory, location, profile),
`support/`(call), `retention/`(member, post), `favorite/`·`review/`(post).

## 아키텍처 계약

- **계층**: Controller → Service(인터페이스) → ServiceImpl → Repository. 컨트롤러에 비즈니스 로직 금지, 컨트롤러에서 Repository 직접 호출 금지.
- **서비스는 항상 인터페이스 먼저** 만들고 `impl/` 아래 구현체를 둔다. 의존성은 생성자 주입만 사용(필드 주입·`@Autowired` 금지).
- **응답은 항상 `BaseResponse<T>`**로 감싼다. 컨트롤러 반환 타입은 `BaseResponse<XxxResponse>`이고 본문은 `BaseResponse.ok(...)`. 본문이 없으면 `BaseResponse<Void>` + `BaseResponse.ok(null)`.
- **에러는 항상 `ApplicationException(XxxStatusCode.YYY)`**로 던진다. 컨트롤러에서 try/catch 하지 않고 `GlobalExceptionHandler`가 처리한다. 새 에러가 필요하면 해당 도메인의 `StatusCode` enum에 항목을 추가한다(코드/메시지/HttpStatus).
- **엔티티 ID**는 `private var id: Long? = null` + `fun getId() = id`. setter를 만들지 않는다.
- **Optional/null**은 `?: throw ApplicationException(...)` 형태로 처리한다.
- 클래스 생성은 Builder 없이 생성자로 한다.
- `@Transactional(rollbackFor = [Exception::class])`를 서비스 구현체에 붙이고, 조회 전용 메서드는 `@Transactional(readOnly = true)`.
- 로그인 사용자는 `@AuthenticationPrincipal memberDetails: MemberDetails`로 받는다.

## 인증/인가

JWT(jjwt) + Spring Security. 새 엔드포인트를 추가하면 **`global/config/SecurityConfig.kt`의 `authorizeHttpRequests` 규칙도 반드시 같이 갱신**한다. 규칙을 빠뜨리면 `anyRequest().authenticated()`에 걸린다. 공개 API가 아니면 `permitAll()`을 쓰지 않는다.

## 빌드 / 실행

```bash
./gradlew build -x test     # 컴파일 확인 (가장 자주 쓰는 검증 수단)
./gradlew bootRun           # 로컬 실행 (.env 필요)
./gradlew test --tests 'spring.springserver.<FQCN>'   # 테스트가 생긴 뒤 단일 실행
```

- 린터·포매터 설정은 없다. 스타일은 `.agents/convention.md`를 사람이/모델이 지키는 방식으로만 강제된다.
- 테스트 하네스는 아직 도입하지 않았다(`src/test` 없음). 의존성(`spring-boot-starter-test`, `mockito-kotlin`)만 선언돼 있다. **요청 없이 테스트 코드를 만들지 않는다.**
- 변경 후 검증은 `./gradlew build -x test` 컴파일 통과로 한다.
- 인프라: PostgreSQL, Redis. 설정은 `src/main/resources/application.yaml`, 값은 `.env`에서 주입.
- 관측: Actuator + Micrometer Prometheus. `monitoring/`에 Prometheus·Grafana·Loki·Promtail·Nginx
  docker-compose 구성이 별도로 있다(서버 배포와 분리된 스택).
- 외부 연동: TossPayments(결제), Solapi(SMS), Agora(음성/영상 통화 토큰), OAuth2 소셜 로그인,
  Cosmos 노드(blockchain), Tika(파일 검사), BouncyCastle(secp256k1), Thymeleaf(메일 템플릿).
- CI/CD: `.github/workflows/deploy.yml` — `main`/`develop` 푸시 시 `gradle test bootJar` → Docker 빌드 → SSH 배포.

## 작업 흐름

1. 관련 도메인의 기존 파일을 먼저 읽어 패턴을 파악한다(가장 가까운 유사 도메인을 복제하듯 따른다).
2. 구현한다.
3. `./gradlew build -x test`로 컴파일을 확인한다.
4. 변경 파일 목록과 남은 리스크를 한국어로 요약한다.

에이전트 자산의 실체는 모두 `.agents/` 아래 한 곳에 있다.

```
.agents/convention.md              컨벤션 단일 출처
.agents/skills/{harness,new-domain,security-review}/SKILL.md
.agents/commands/{convention,domain,security}.md
.claude/skills   -> ../.agents/skills     (심링크, Claude Code가 스캔하는 경로)
.claude/commands -> ../.agents/commands   (심링크)
.claude/settings.json                     Claude Code 권한 설정 (도구 전용, 이전 불가)
.codex/config.toml                        Codex 설정 (도구 전용, 이전 불가)
```

`harness`(코드 작성), `new-domain`(도메인·엔드포인트 추가), `security-review`(보안 리뷰) 스킬과
대응 슬래시 커맨드 `/convention`, `/domain`, `/security`가 있다. 해당 작업이면 그 절차를 따른다.
스킬·커맨드를 고칠 때는 반드시 `.agents/` 쪽 실체 파일을 고친다.

## Git

- 기본 브랜치 `main`, 작업 브랜치 `develop` 및 `<이슈번호>-<타입>-<설명>` (예: `130-fix-merge-oauth-package-with-auth`).
- 커밋 메시지: `feat: `, `fix: `, `refactor: ` 등 + 한국어 설명.
- 커밋·푸시는 사용자가 요청할 때만 한다.
