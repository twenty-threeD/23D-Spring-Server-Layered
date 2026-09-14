---
description: 변경된 코드가 프로젝트 컨벤션을 지키는지 검사한다
argument-hint: "[파일 경로 | 비우면 git 변경분 전체]"
allowed-tools: Read, Grep, Glob, Bash(git diff:*), Bash(git status:*)
---

`.agents/convention.md`를 먼저 읽고, 아래 대상 코드에서 컨벤션 위반을 찾아 보고해라.

대상: $1

대상이 비어 있으면 `git status`와 `git diff HEAD`로 변경된 파일을 찾아 그 파일들을 검사한다.

보고 형식(한국어):

| 파일:라인 | 위반한 규칙 | 현재 | 수정안 |

- 실제로 파일을 읽고 확인한 위반만 보고한다. 추측 금지.
- 위반이 없으면 "위반 없음"이라고만 답한다.
- 수정은 사용자가 요청할 때만 적용한다.
