---
description: 보안 리뷰를 수행한다 (인가/JWT/입력검증/비밀값)
argument-hint: "[도메인 이름 또는 파일 경로 | 비우면 git 변경분]"
allowed-tools: Read, Grep, Glob, Bash(git diff:*), Bash(git status:*)
---

`security-review` 스킬의 절차에 따라 보안 리뷰를 수행해라.

대상: $ARGUMENTS

대상이 비어 있으면 `git diff HEAD`의 변경분을 대상으로 한다.
발견은 심각도 순으로 정렬해 한국어로 보고하고, 수정은 요청받을 때만 적용한다.
