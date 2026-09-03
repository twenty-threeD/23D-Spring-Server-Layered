---
name: new-domain
description: 새 도메인(패키지)이나 새 API 엔드포인트를 추가할 때 사용한다. 이 저장소의 레이어드 패키지 구조와 파일 생성 순서를 따른다.
---

# 신규 도메인 / 엔드포인트 추가 절차

## 0. 사전 확인

- `.Codex/convention.md`를 읽는다.
- 가장 유사한 기존 도메인을 하나 골라(예: 단순 CRUD면 `post`, 인증 관련이면 `auth`) 그 파일들을 읽고 패턴을 그대로 따른다.
- 도메인 이름, 엔드포인트 경로, 인가 수준(공개/USER)을 사용자에게 확인받고 시작한다.

## 1. 생성 순서

`src/main/java/spring/springserver/domain/<도메인>/` 아래에 아래 순서로 만든다.

1. `entity/<Xxx>.kt` — JPA 엔티티. ID는 `private var id: Long? = null` + `fun getId() = id`.
2. `exception/<Xxx>StatusCode.kt` — `StatusCode` 구현 enum (코드, 메시지, HttpStatus).
3. `repository/<Xxx>Repository.kt` — `JpaRepository<Xxx, Long>`.
4. `data/request/<동작>Request.kt` — Bean Validation 어노테이션 포함. 필요하면 `toEntity()`.
5. `data/response/<동작>Response.kt` — `companion object { fun of(...) }` 팩토리.
6. `service/<Xxx>Service.kt` — 인터페이스.
7. `service/impl/<Xxx>ServiceImpl.kt` — `@Service`, `@Transactional(rollbackFor = [Exception::class])`, 생성자 주입.
8. `controller/<Xxx>Controller.kt` — `@RestController`, `@RequestMapping("/api/<도메인>")`, 반환은 `BaseResponse<...>`.

## 2. 반드시 같이 갱신할 것

- **`global/config/SecurityConfig.kt`** — 새 경로의 인가 규칙 추가. 빠뜨리면 인증 필수로 동작한다.
- 공개 API가 아니면 `permitAll()`을 쓰지 않는다.

## 3. 검증 및 보고

- `./gradlew build -x test`로 컴파일 확인.
- 생성한 파일 경로 전부와 SecurityConfig 변경 내용을 한국어로 요약한다.
- 테스트 코드는 사용자가 요청할 때만 작성한다.
