---
name: security-review
description: 이 저장소의 Kotlin + Spring Boot 코드에 대해 보안 리뷰를 수행할 때 사용한다. 인증·인가, 입력 검증, 비밀값 노출, 주입 취약점을 점검한다.
---

# 보안 리뷰 하네스

## 점검 항목 (이 프로젝트 특화 순서)

1. **인가 규칙**: 새/변경된 엔드포인트가 `global/config/SecurityConfig.kt`에 반영됐는가.
   불필요한 `permitAll()`, 과도한 `hasRole`, 누락되어 `anyRequest().authenticated()`에 의도치 않게 걸리는 경로를 찾는다.
   `corsFilter()`는 `allowedOriginPattern("*") + allowCredentials = true`라 운영 환경에서는 위험 — 출처 화이트리스트로 좁힐 것을 권고한다.
2. **JWT**: `global/jwt/` — 만료 검증, 토큰 타입(access/refresh) 구분, 쿠키 플래그(HttpOnly/Secure/SameSite), 시크릿 길이·주입 경로.
3. **소유권 검사**: 리소스 수정/삭제 시 "로그인 여부"만 보고 "본인 리소스인지"를 안 보는 IDOR 패턴.
4. **입력 검증**: 요청 DTO에 Bean Validation, 컨트롤러에 `@Valid`. 파일 업로드는 확장자·MIME(tika)·크기 검증.
5. **주입**: `@Query` JPQL 문자열 결합, MongoDB 쿼리 조립, 네이티브 쿼리 파라미터 바인딩.
6. **비밀값 노출**: 하드코딩된 키, 로그에 찍히는 토큰/비밀번호/PII, 예외 메시지의 내부 정보 노출.
   `application.yaml`의 `springdoc.*.enabled: true`는 배포 시 false 여부를 확인한다.
7. **암호**: `key/` 도메인의 개인키가 평문 저장되는지(현재 `MemberKey.privateKey`) — 저장 시 암호화 권고.
8. **의존성**: `../../../build.gradle.kts`에 알려진 취약 버전이 보이면 지적한다.

## 보고 형식

발견마다 다음을 한국어로 제시한다.

```
[심각도: Critical | High | Medium | Low] 제목
- 위치: 파일:라인
- 문제: 무엇이 왜 위험한가
- 공격 시나리오: 구체적 입력/상태 → 결과
- 수정: 적용 가능한 구체적 코드/설정
```

추측성 지적은 하지 않는다. 코드를 실제로 읽고 확인한 것만 보고하고, 확신이 없으면 "확인 필요"로 명시한다.
수정은 사용자가 요청할 때만 적용한다.
