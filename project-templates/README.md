# project-templates

project-generator가 프로젝트를 만들 때 **반드시 따라야 하는** 템플릿, 규칙, 에이전트 정의를 모아 둔 폴더입니다.
생성기 코드와 같은 저장소에 있어서, 같은 커밋의 생성기는 누가 실행해도 같은 템플릿으로 생성합니다.
템플릿 버전(예: `v1.0.0`)은 생성기의 `generator.template-version`에 적고, 생성된 프로젝트의 문서에 기록됩니다.

## 구조

```
project-templates/
├── README.md                  # 이 문서: 구조, 버전 정책, 템플릿 문법
├── agents.md                  # 파이프라인과 서브에이전트별 역할·입력·출력
├── rules.md                   # 코딩 컨벤션, 네이밍, 타입 매핑, 교차 검증 규칙
├── skills/                    # 서브에이전트 시스템 프롬프트
│   ├── planner.md
│   ├── architect.md
│   └── api.md
├── schemas/                   # 에이전트 출력 JSON 형식 (JSON Schema 2020-12)
│   ├── prd.schema.json
│   ├── architecture.schema.json
│   └── api.schema.json
├── templates/
│   └── spring-boot/           # 생성될 프로젝트의 보일러플레이트
│       ├── template.json      # 어떤 템플릿 파일을 어디에 렌더링할지 정의
│       ├── root/              # 프로젝트 단위 파일 (빌드 설정, Application 등)
│       ├── domain/            # 엔티티마다 반복되는 파일
│       ├── common/            # 공통 코드 (예외 처리)
│       ├── docs/              # PRD, 아키텍처, API 명세 문서
│       └── static/            # 렌더링 없이 복사하는 파일 (Gradle wrapper)
└── examples/
    └── todo-service/
        ├── input.json         # 입력 (프로젝트명 + 요구사항)
        ├── prd.json           # PlannerAgent 출력
        ├── architecture.json  # ArchitectAgent 출력
        ├── api.json           # ApiAgent 출력
        └── expected/          # 위 JSON으로 렌더링한 정답 결과 (렌더러 테스트용, static 제외)
```

## 버전 정책

| 변경 내용 | 버전 |
|---|---|
| `schemas/` 필드 삭제·이름 변경 등 기존 JSON과 호환되지 않는 변경 | MAJOR (`v2.0.0`) |
| 템플릿 파일 추가, 규칙 추가, 스키마에 선택 필드 추가 | MINOR (`v1.1.0`) |
| 오타, 문서 수정, 생성 결과가 바뀌지 않는 수정 | PATCH (`v1.0.1`) |

생성 결과가 조금이라도 바뀌는 변경은 PATCH가 아니라 MINOR 이상으로 올립니다. 그래야 "같은 버전 = 같은 결과"가 지켜집니다.

## 템플릿 문법

[Mustache](https://mustache.github.io/mustache.5.html)의 기본 문법만 사용합니다. 로직은 템플릿에 넣지 않고, 필요한 값은 렌더러가 미리 계산해서 넘깁니다(`rules.md`의 "렌더링 변수" 참고).

| 문법 | 의미 |
|---|---|
| `{{name}}` | 값 출력 |
| `{{#list}} ... {{/list}}` | 목록 반복, 또는 값이 true일 때만 출력 |
| `{{^flag}} ... {{/flag}}` | 값이 false이거나 목록이 비었을 때만 출력 |

렌더러 구현 시 지켜야 할 것:

- **HTML 이스케이프를 끕니다.** 켜 두면 `List<TodoResponse>`의 `<`, `>`가 `&lt;`, `&gt;`로 바뀝니다.
- 태그만 있는 줄(standalone tag)은 줄째로 제거합니다(Mustache 표준 동작).
- 줄바꿈은 항상 `\n`, 줄 끝 공백은 제거하고, 파일 끝에는 줄바꿈 하나를 둡니다.
- `template.json`의 `condition`이 있으면 그 값이 true인 엔티티만 렌더링합니다. `static`의 파일은 렌더링하지 않고 그대로 복사합니다.

## 새 템플릿 추가하기

1. `templates/<이름>/`에 템플릿 파일을 만듭니다(확장자 `.mustache`).
2. `template.json`의 `files`에 `source`, `target`, `scope`를 추가합니다.
3. `examples/`의 예시로 렌더링해 보고 결과를 확인합니다.
4. 버전 정책에 맞게 `template-version`을 올립니다.
