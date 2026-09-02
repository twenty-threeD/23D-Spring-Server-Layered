---
name: harness
description: 이 저장소에서 Kotlin + Spring Boot 프로덕션 코드를 작성하거나 수정할 때 사용한다. 컨벤션 준수, 계층 구조, 예외/응답 규약을 강제한다.
---

# 코드 작성 하네스 (Kotlin / Spring Boot)

## 시작 전 (필수)

1. `.Codex/convention.md`를 읽는다. 이 파일이 컨벤션의 단일 출처(Single Source of Truth)다.
2. 손댈 도메인의 기존 파일(Controller / Service / ServiceImpl / DTO / StatusCode)을 먼저 읽는다.
3. 요청이 컨벤션과 충돌하면 먼저 지적하고, 사용자가 명시적으로 뒤집지 않는 한 컨벤션을 따른다.

## 코드 작성 규칙

- 계층: Controller → Service(인터페이스) → ServiceImpl → Repository. 컨트롤러에 로직을 두거나 Repository를 직접 호출하지 않는다.
- 인터페이스를 먼저 만들고 `impl/` 아래 구현체를 둔다. 의존성은 생성자 주입만.
- 반환은 항상 `BaseResponse.ok(...)` + `XxxResponse.of(...)`.
- 실패는 항상 `throw ApplicationException(XxxStatusCode.YYY)`. 필요한 코드가 없으면 해당 도메인 StatusCode enum에 추가한다.
- 조회 실패는 `?: throw ApplicationException(...)` 패턴. `!!` 남용 금지.
- 타입 추론이 되면 타입 선언을 생략한다. 변수는 `val` 우선.
- 새 엔드포인트를 만들면 `global/config/SecurityConfig.kt`의 인가 규칙을 같은 커밋에서 갱신한다.
- 공개 API 시그니처를 바꿔야 하면 먼저 알린다.
- 테스트 코드는 사용자가 명시적으로 요청할 때만 작성한다(현재 이 저장소에 테스트 하네스는 없다).

## 자주 어기는 컨벤션 (체크리스트)

- [ ] 여는 중괄호 뒤 빈 줄 한 줄
- [ ] `return` 문 앞 빈 줄
- [ ] 파일 마지막 닫는 중괄호 뒤 빈 줄 없음
- [ ] 파라미터가 1개여도 여는 괄호 뒤 줄바꿈, 값이 2개 이상이면 각 줄에 하나씩
- [ ] `): 인터페이스 {` — 콜론은 닫는 괄호 바로 뒤
- [ ] 짧은 어노테이션 위, 긴 어노테이션 아래
- [ ] 파라미터 이름은 타입명을 camelCase로 (`signUpRequest: SignUpRequest`)
- [ ] DTO 파라미터가 `HttpServletRequest/Response`보다 앞
- [ ] 컨트롤러의 `@RequestBody` DTO에 `@Valid`
- [ ] 엔티티 ID: `private var id: Long? = null` + `fun getId() = id`, setter 없음

## 마무리

1. `./gradlew build -x test`로 컴파일을 확인한다.
2. 생성/수정한 파일 경로를 전부 나열한다.
3. 남은 리스크와 후속 제안을 한국어로 요약한다.
