# project-generator

요구사항과 프로젝트명을 입력하면, git에 저장된 템플릿과 가이드라인을 바탕으로 AI 에이전트가 기획서(PRD), 아키텍처, API 명세를 구조화하고 일관된 프로젝트를 로컬에 생성합니다.

## 기술 스택

- Java 21, Spring Boot 3.5
- Spring AI 1.1 (OpenAI 호환 API: Ollama, Gemini 등)
- Slack Bolt (Socket Mode)
- JGit (템플릿 저장소 관리)
- MySQL, Spring Data JPA (의존성만 있고 아직 쓰지 않아 꺼 둠, `application.yml`의 `spring.autoconfigure.exclude`)

## 사전 준비

### 1. JDK 21

### 2. Ollama (로컬 AI, 무료)

```bash
brew install ollama
ollama serve
ollama pull qwen2.5-coder:7b
```

## 실행

```bash
./gradlew bootRun
```

브라우저에서 http://localhost:8080 을 열고 프로젝트명과 요구사항을 입력한 뒤 **생성**을 누릅니다.
로컬 AI로 기획서(PRD) → 아키텍처 → API 설계를 거쳐 `~/generated-projects/<프로젝트명>`에 프로젝트가 생성됩니다(보통 1~3분).

- 생성 작업은 한 번에 하나씩 순서대로 실행됩니다. 웹과 Slack 요청이 같은 순서를 공유합니다.
- 작업 목록은 메모리에만 있어서 앱을 다시 시작하면 사라집니다. 생성된 프로젝트 폴더는 남습니다.
- 같은 이름의 폴더가 이미 있으면 생성하지 않습니다.

## 템플릿

보일러플레이트, 디렉토리 구조, 코딩 규칙, 에이전트 지침(`agents.md`, `rules.md`, `skills/`)은 이 저장소의 [`project-templates/`](project-templates/README.md)에 있습니다.
템플릿이 생성기 코드와 같은 커밋에 들어 있으므로, **같은 커밋의 생성기는 누가 실행해도 같은 템플릿을 씁니다.**

템플릿을 바꾸는 순서:

1. `project-templates/`의 템플릿이나 규칙을 고칩니다.
2. `examples/*/expected/`(정답 결과)도 같이 고치고 `./gradlew test`로 확인합니다.
3. 생성 결과가 바뀌는 변경이면 [버전 정책](project-templates/README.md#버전-정책)에 맞게 `application.yml`의 `template-version`을 올립니다. 이 값은 생성된 프로젝트의 문서에 기록됩니다.
4. 템플릿, 정답 결과, 버전을 한 커밋에 넣습니다.

> 나중에 템플릿을 여러 생성기나 팀이 공유하게 되면 `project-templates/`를 별도 git 저장소로 옮기고 `GENERATOR_TEMPLATES_REPOSITORY`에 주소를 넣으면 됩니다. 그러면 생성기가 그 저장소의 `template-version` **태그**를 JGit으로 받아 `~/.project-generator/templates/`에 캐시해 씁니다. 브랜치 이름은 받지 않습니다.

### API

| 메서드 | 경로 | 설명 |
|---|---|---|
| `POST` | `/api/generations` | 생성 요청 접수 (`{"projectName", "requirements"}`) → `202` |
| `GET` | `/api/generations/{id}` | 진행 상황과 결과 |
| `GET` | `/api/generations` | 최근 작업 20개 |

## Slack에서 쓰기

```
/generate todo-service 할 일을 등록, 수정, 삭제하고 카테고리로 분류하는 서비스
```

입력한 사람에게만 "접수했습니다"가 바로 보이고, 생성이 끝나면 결과 요약(엔티티, API 경로, 생성 위치)이 채널에 올라옵니다.
**Socket Mode**를 쓰기 때문에 공개 주소나 ngrok 같은 터널이 필요 없습니다. 생성기가 Slack에 먼저 접속합니다.

> 프로젝트는 **생성기를 실행 중인 PC**에 만들어집니다. Slack으로 요청한 사람이 다른 PC에 있으면 그 사람의 PC에는 생기지 않습니다.

### Slack 앱 만들기

1. https://api.slack.com/apps → **Create New App** → **From a manifest** → 워크스페이스 선택 → 아래 YAML 붙여넣기

   ```yaml
   display_information:
     name: Project Generator
   features:
     bot_user:
       display_name: Project Generator
     slash_commands:
       - command: /generate
         description: 요구사항으로 프로젝트를 생성합니다
         usage_hint: "<프로젝트명> <요구사항>"
   oauth_config:
     scopes:
       bot:
         - commands
   settings:
     socket_mode_enabled: true
   ```

2. **Basic Information** → **App-Level Tokens** → **Generate Token and Scopes** → scope `connections:write` 추가 → 생성된 `xapp-...` 토큰 복사
3. **Install App** → **Install to Workspace** → 생성된 **Bot User OAuth Token** `xoxb-...` 복사
4. 두 토큰을 넣고 실행

   ```bash
   SLACK_BOT_TOKEN=xoxb-... SLACK_APP_TOKEN=xapp-... ./gradlew bootRun
   ```

   로그에 `Slack Socket Mode 접속: /generate 명령을 받습니다`가 나오면 준비 완료입니다. 토큰이 없으면 Slack 연동만 꺼지고 웹 화면은 그대로 쓸 수 있습니다.

생성기 PC에서 파일을 만드는 명령이므로, 쓸 수 있는 사람을 제한하려면 `SLACK_ALLOWED_USER_IDS`에 Slack 사용자 ID를 쉼표로 넣으세요(프로필 → ⋮ → **멤버 ID 복사**).

## 테스트

테스트는 `project-templates/`의 템플릿과 예시(`examples/todo-service/expected/`)로 돕니다.

```bash
./gradlew test                                                          # 실제 AI 없이 (가짜 LLM)
GENERATOR_LLM_IT=true ./gradlew test --tests '*LlmIntegrationTest' -i   # 실제 AI로 전체 파이프라인
```

## 환경변수

| 이름 | 기본값 | 설명 |
|---|---|---|
| `AI_BASE_URL` | `http://localhost:11434` | OpenAI 호환 API 주소 |
| `AI_API_KEY` | `ollama` | API 키 (Ollama는 검사하지 않음) |
| `AI_MODEL` | `qwen2.5-coder:7b` | 사용할 모델 |
| `GENERATOR_OUTPUT_DIR` | `~/generated-projects` | 프로젝트가 생성될 폴더 |
| `GENERATOR_TEMPLATES_DIR` | `project-templates` | 템플릿 폴더 |
| `GENERATOR_TEMPLATE_VERSION` | `v1.0.0` | 템플릿 버전 (생성 결과에 기록) |
| `GENERATOR_TEMPLATES_REPOSITORY` | (없음) | 템플릿을 별도 git 저장소로 분리했을 때만. 설정하면 그 저장소의 `GENERATOR_TEMPLATE_VERSION` 태그를 받아 씀 |
| `GENERATOR_TEMPLATES_CACHE_DIR` | `~/.project-generator/templates` | 저장소에서 받은 템플릿 캐시 |
| `GENERATOR_WEB_URL` | `http://localhost:8080` | Slack 메시지의 "웹에서 보기" 링크 주소 |
| `SLACK_BOT_TOKEN` | (없음) | Slack 봇 토큰 `xoxb-...` |
| `SLACK_APP_TOKEN` | (없음) | Slack 앱 수준 토큰 `xapp-...` (`connections:write`) |
| `SLACK_ALLOWED_USER_IDS` | (없음 = 누구나) | `/generate`를 쓸 수 있는 Slack 사용자 ID, 쉼표로 구분 |
| `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD` | | MySQL (DB 자동 설정을 다시 켰을 때만 사용) |
