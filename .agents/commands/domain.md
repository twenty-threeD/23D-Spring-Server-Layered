---
description: 레이어드 구조에 맞춰 새 도메인/엔드포인트를 추가한다
argument-hint: "<도메인 이름> [설명]"
---

`new-domain` 스킬의 절차에 따라 아래 도메인을 추가해라.

요청: $ARGUMENTS

시작 전에 엔드포인트 경로와 인가 수준(공개 / ROLE_USER)을 확인받고, `SecurityConfig.kt` 갱신을 잊지 마라.
