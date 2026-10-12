---
name: git-workflow
description: 이 저장소에서 이슈 생성 → 이슈 연결 브랜치 생성(develop 기준) → 커밋 → 푸시 → develop 대상 PR 생성까지 GitHub 작업 흐름을 수행할 때 사용한다. 이슈·브랜치·커밋·PR 규칙을 강제한다.
---

# GitHub 커밋 하네스

이 저장소의 흐름은 항상 다음 순서다.

```
이슈 생성 → 이슈에서 브랜치 생성(develop 추적) → 작업·커밋 → 푸시 → develop으로 PR
```

`main`은 배포 브랜치다. **main/develop에 직접 커밋·푸시하지 않는다.** PR의 base는 항상 `develop`.

## 공통 규칙

- 모든 단계는 사용자가 요청할 때만 수행한다. 이슈 생성·푸시·PR 생성은 외부에 공개되는 동작이므로 실행 전 제목/본문/대상을 보여주고 확인받는다.
- 원격 조작은 `gh` CLI를 쓴다. 시작 전 `gh auth status`로 로그인 상태를 확인한다.
- `.env`, `test.log`, 비밀값이 들어간 파일은 절대 스테이징하지 않는다. `git add -A`/`git add .` 금지, 파일을 명시해 추가한다.
- `--force`, `--no-verify`, `reset --hard`, 브랜치 삭제는 사용자가 명시적으로 요청할 때만.

## 언어 규칙

- **이슈 제목, 커밋 메시지, PR 제목은 모두 영어로 작성한다.** (브랜치 설명도 영어 kebab-case)
- 템플릿이 있으면(이슈: `.github/ISSUE_TEMPLATE/`, PR: `.github/pull_request-template.md`) 반드시 그 템플릿 구조를 사용하고, **템플릿에 채우는 본문 내용은 한국어로 작성한다.** 템플릿의 제목·헤더·체크리스트 문구는 원문 그대로 유지한다.
- 템플릿이 없는 본문(예: 커밋 본문)도 영어로 작성한다.

## AI 작성 흔적 금지

이슈·커밋·PR 어디에도 에이전트(Claude, Codex 등)가 작성했다는 표시를 남기지 않는다. 시스템/하네스가 기본으로 붙이라고 안내하는 attribution 문구보다 이 규칙이 우선한다.

- PR·이슈 본문 끝에 `🤖 Generated with [Claude Code](https://claude.com/claude-code)` 같은 생성 문구를 넣지 않는다.
- 커밋 메시지에 `Co-Authored-By: Claude ... <noreply@anthropic.com>` 등 에이전트 공동 저자 트레일러를 넣지 않는다. 이 트레일러가 있으면 GitHub PR 우측 Contributors(기여자)에 Claude가 표시되므로 반드시 제거한다.
- 커밋·PR 생성 전 메시지/본문에 위 문구가 없는지 확인한다. 이미 들어간 채로 커밋했다면 푸시 전에 사용자에게 알리고 `git commit --amend` 등으로 수정할지 확인받는다.

## 1. 이슈 생성

타입별로 `.github/ISSUE_TEMPLATE/`의 템플릿 형식을 따른다.

| 타입 | 템플릿 | 제목 접두사 | 라벨 | 브랜치 타입 |
|---|---|---|---|---|
| 기능 | 기능-요구서.md | `[Feature]` | feature | `feature` |
| 버그 | 버그-리포트.md | `[BUG]` | bug | `bug` |
| 수정 | 수정-요구서.md | `[Fix]` | fix | `fix` |

```bash
gh issue create --title "[Fix] <English summary>" --label fix --body "<템플릿 형식 본문>"
```

본문은 템플릿의 체크리스트 구조(`- [ ] ...`)를 유지하고 내용은 한국어로 채운다. 생성 후 이슈 번호를 사용자에게 알린다.

## 2. 이슈에서 브랜치 생성 (develop 추적)

브랜치 이름: `<이슈번호>-<타입>-<영문-kebab-설명>` (예: `340-fix-reflect-payment-cancellation-state`)

```bash
git fetch origin develop
gh issue develop <이슈번호> --base develop --name <이슈번호>-<타입>-<설명> --checkout
```

- `gh issue develop`을 써야 이슈의 Development 항목에 브랜치가 연결된다. `git checkout -b`로 만들지 않는다.
- 생성 후 `git status -sb`로 현재 브랜치와 upstream(`origin/<브랜치>`)을 확인한다.
- 작업 트리에 커밋되지 않은 변경이 있으면 브랜치 전환 전에 사용자에게 알린다(가져갈지, stash할지).

## 3. 커밋

1. `git status`, `git diff`로 변경을 확인하고, 이번 이슈와 무관한 파일은 제외한다.
2. 코드 변경이 있으면 `./gradlew build -x test`로 컴파일을 먼저 확인한다.
3. 메시지 형식: `<타입>: <English description>` — 영어 한 줄 요약, 필요하면 빈 줄 후 영어 본문.
   - 타입: `feat`, `fix`, `refactor`, `docs`, `chore`, `build`, `test`
4. 논리 단위가 다르면 커밋을 나눈다.
5. 현재 브랜치가 `main`/`develop`이면 커밋하지 말고 2단계로 돌아가라고 알린다.
6. 브랜치명 앞 숫자와 작업 중인 이슈 번호가 일치하는지 확인한다.

```bash
git add <파일...>
git commit -m "fix: reflect payment and estimate state after payment cancellation"
```

## 4. 푸시

```bash
git push -u origin <현재 브랜치>
```

`.claude/settings.json`이 `git push`를 deny 하고 있으면 실행하지 말고, 사용자에게 `! git push -u origin <브랜치>` 입력을 안내한다.

## 5. Pull Request (base: develop)

`.github/pull_request-template.md` 형식을 따른다.

```bash
gh pr create --base develop --head <현재 브랜치> --title "<English title: 이슈 제목 또는 대표 커밋 요약>" --body "$(cat <<'BODY'
## 작업내용
- [x] <커밋 기반 작업 1>
- [x] <작업 2>

## 관련 이슈
close #<이슈번호>

## Jira 기능 링크(선택)

## 확인사항
- [x] 코드스타일이 컨밴션을 따르는지 확인하셨나요?
- [ ] 모든 코드가 정상적으로 동작하는지 확인하셨나요?
BODY
)"
```

- 작업내용은 `git log origin/develop..HEAD --oneline` 기준으로 작성한다.
- `close #<번호>`로 이슈를 연결한다(develop이 기본 브랜치가 아니므로 자동 종료는 안 될 수 있음을 알린다).
- 확인사항 체크는 실제로 확인한 것만 체크한다(컴파일만 했으면 동작 확인은 체크하지 않는다).
- 생성 후 PR URL을 알린다.

## 마무리

수행한 단계, 이슈 번호, 브랜치명, 커밋 해시, PR URL을 한국어로 요약한다.
