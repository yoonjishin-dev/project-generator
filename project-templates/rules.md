# rules.md

생성되는 프로젝트가 지켜야 할 규칙입니다. "에이전트 공통 규칙"은 모든 에이전트 프롬프트에 그대로 붙고, 나머지는 렌더러와 템플릿이 지킵니다.

## 에이전트 공통 규칙

- 출력은 주어진 JSON Schema를 따르는 JSON 하나뿐입니다. 앞뒤에 설명이나 ``` 표시를 붙이지 않습니다.
- 사람이 읽는 값(`title`, `summary`, `label`, `description` 등)은 **한국어**로 씁니다.
- 식별자(엔티티명, 필드명, 경로)는 **영어**로 쓰고 아래 네이밍 규칙을 따릅니다.
- 요구사항에 없는 기능을 추측해서 추가하지 않습니다. 애매하면 더 작은 범위를 고르고, 뺀 내용은 PRD의 `outOfScope`에 적습니다.
- 목록은 아래 정렬 규칙에 맞춰 정렬해서 출력합니다.

## 네이밍

| 대상 | 형식 | 예시 |
|---|---|---|
| 프로젝트명(입력) | kebab-case, 영문 소문자로 시작 | `todo-service` |
| 기본 패키지 | `com.generated.` + 프로젝트명에서 `-` 제거 | `com.generated.todoservice` |
| 엔티티 | PascalCase, 단수형 | `Todo`, `Category` |
| 필드, 관계 필드 | camelCase | `dueDate`, `category` |
| 도메인 패키지 | 엔티티명 소문자 | `todo`, `category` |
| 테이블 | 엔티티명 snake_case | `todo`, `order_item` |
| 컬럼 | 필드명 snake_case | `due_date` |
| API 경로 | `/api/` + kebab-case 복수형 | `/api/todos`, `/api/order-items` |

**예약어**: 필드명으로 `id`, `createdAt`, `updatedAt`은 쓸 수 없습니다(템플릿이 자동으로 만듦). Java 예약어(`class`, `order` 등 SQL 예약어 포함)도 피합니다.

## 정렬 규칙

생성 결과를 항상 같게 만들기 위해, 렌더러는 렌더링 전에 다음 순서로 정렬합니다. 에이전트도 같은 순서로 출력합니다.

- `features`: `id` 오름차순
- `entities`: 관계 의존 순서(참조되는 엔티티가 먼저) → 같은 단계면 `name` 오름차순
- `fields`: 에이전트가 출력한 순서 유지 (화면 표시 순서이므로 정렬하지 않음)
- `relations`: `fieldName` 오름차순
- `resources`: `entities`와 같은 순서
- `operations`: `LIST, GET, CREATE, UPDATE, DELETE` 순서

## 필드 타입 매핑

| 스키마 `type` | Java 타입 | JPA 컬럼 | ER 다이어그램 | 검증 (required일 때) |
|---|---|---|---|---|
| `STRING` | `String` | `length = 255` | `varchar` | `@NotBlank` |
| `TEXT` | `String` | `columnDefinition = "TEXT"` | `text` | `@NotBlank` |
| `INTEGER` | `Integer` | | `int` | `@NotNull` |
| `LONG` | `Long` | | `bigint` | `@NotNull` |
| `DECIMAL` | `BigDecimal` | `precision = 19, scale = 2` | `decimal` | `@NotNull` |
| `BOOLEAN` | `Boolean` | | `boolean` | `@NotNull` |
| `DATE` | `LocalDate` | | `date` | `@NotNull` |
| `DATETIME` | `LocalDateTime` | | `datetime` | `@NotNull` |

- `required: true`면 컬럼에 `nullable = false`를 붙입니다.
- `unique: true`면 컬럼에 `unique = true`를 붙입니다.
- 관계는 `MANY_TO_ONE`만 지원합니다. `@ManyToOne(fetch = FetchType.LAZY)` + `@JoinColumn(name = "<fieldName snake_case>_id")`로 만듭니다.

## 코드 컨벤션

- **기술 스택**: Java 21, Spring Boot 3.5.6, Spring Data JPA, MySQL, Lombok.
- **패키지 구조**: 도메인별 패키지 (`<기본 패키지>.<도메인>`), 공통 코드는 `<기본 패키지>.common`.
- **도메인 하나의 파일**: `Entity`, `Repository`, `Service`, `Controller`, `Request`(record), `Response`(record).
- **엔티티**: `@Getter`, `@NoArgsConstructor(access = AccessLevel.PROTECTED)`만 씁니다. `@Setter`, `@Data`는 쓰지 않고, 변경은 `update(...)` 메서드로만 합니다.
- **DTO**: Java `record`. 엔티티를 컨트롤러 밖으로 직접 반환하지 않습니다.
- **트랜잭션**: Service 클래스에 `@Transactional(readOnly = true)`, 쓰기 메서드에만 `@Transactional`.
- **의존성 주입**: 생성자 주입(`@RequiredArgsConstructor` + `private final`).
- **응답 코드**: 생성 `201`, 삭제 `204`, 검증 실패 `400`, 없는 ID `404`, 고유 값 중복·참조 중인 데이터 삭제 `409` (모두 ProblemDetail).
- **import**: 와일드카드는 `jakarta.persistence.*`, `jakarta.validation.constraints.*`, `org.springframework.web.bind.annotation.*`에만 허용합니다.
- **들여쓰기**: 공백 4칸, 줄 끝 공백 없음.

## 교차 검증 규칙

스키마만으로는 확인할 수 없는 규칙입니다. 하나라도 어기면 해당 에이전트에 재요청합니다(`agents.md` "실패 처리").

| 대상 | 규칙 |
|---|---|
| prd | 기능 `id`가 서로 겹치지 않음 |
| architecture | 엔티티 `name`이 서로 겹치지 않음 |
| architecture | 한 엔티티 안에서 필드 `name`과 관계 `fieldName`이 서로 겹치지 않음 |
| architecture | 관계가 있으면 `<관계 fieldName>Id` 필드를 따로 만들지 않음 (예: `category` 관계 + `categoryId` 필드 금지) |
| architecture | 한 엔티티 안에서 컬럼 이름(`id`, `created_at`, `updated_at`, 필드 snake_case, `<관계>_id`)이 겹치지 않음 |
| architecture | 관계 `target`이 존재하는 엔티티 |
| architecture | 한 엔티티 안에서 같은 `target`을 두 번 참조하지 않음 |
| architecture | 관계에 순환이 없음 (A→B→A 금지, 자기 자신 참조 금지) |
| architecture | `featureIds`의 모든 값이 PRD `features`에 있음 |
| api | 모든 엔티티에 리소스가 정확히 하나씩 있음 |
| api | 리소스 `path`가 서로 겹치지 않음 |

## 렌더링 변수

템플릿에서 쓸 수 있는 값입니다. JSON 원본 값에 렌더러가 계산한 값을 더해서 넘깁니다.

**프로젝트 단위** (`scope: project`, 모든 파일에서 사용 가능)

| 변수 | 예시 | 출처 |
|---|---|---|
| `projectName` | `todo-service` | 입력 |
| `basePackage` | `com.generated.todoservice` | 계산 |
| `basePackagePath` | `com/generated/todoservice` | 계산 |
| `databaseName` | `todo_service` | 계산 (`-`를 `_`로) |
| `entityPackagesText` | `` `com.generated.todoservice.category`, ... `` | 계산 |
| `templateVersion` | `v1.0.0` | 템플릿 버전 |
| `prd` | `prd.json` 전체 | PlannerAgent |
| `entities` | 아래 엔티티 변수 목록 | architecture + api 병합 |

**엔티티 단위** (`scope: entity`, 또는 `{{#entities}}` 안)

| 변수 | 예시 | 출처 |
|---|---|---|
| `name`, `label`, `description` | `Todo`, `할 일` | architecture |
| `varName` | `todo` | 계산 (첫 글자 소문자) |
| `packageName` | `todo` | 계산 (소문자) |
| `tableName` | `todo` | 계산 (snake_case) |
| `path` | `/api/todos` | api |
| `hasList`, `hasGet`, `hasCreate`, `hasUpdate`, `hasDelete` | `true` | api `operations` |
| `hasBody` | `true` | 계산 (`hasCreate` 또는 `hasUpdate`) |
| `hasId` | `true` | 계산 (`hasGet`, `hasUpdate`, `hasDelete` 중 하나) |
| `usesNotFound` | `true` | 계산 (`hasId`, 또는 `hasBody`이면서 `hasRelations`) |
| `importsHttpStatus` | `true` | 계산 (`hasCreate` 또는 `hasDelete`) |
| `featureIdsText` | `F-001, F-002` | 계산 |
| `hasRelations` | `true` | 계산 |
| `importsBigDecimal`, `importsLocalDate`, `importsLocalDateTime` | `false` | 계산 (필드 타입) |
| `fields` | 아래 필드 변수 목록 | architecture |
| `relations` | 아래 관계 변수 목록 | architecture |
| `params` | 필드 + 관계를 합친 생성자·DTO 인자 목록 | 계산 |

**필드 단위** (`{{#fields}}` 안)

| 변수 | 예시 |
|---|---|
| `name`, `label`, `type`, `required`, `unique` | `dueDate`, `마감일`, `DATE`, `false`, `false` |
| `capitalizedName` | `DueDate` |
| `javaType` | `LocalDate` |
| `columnName` | `due_date` |
| `columnAnnotation` | `@Column(name = "due_date")` |
| `erType` | `date` |
| `validationAnnotation` | `@NotNull ` (뒤 공백 포함, 없으면 빈 문자열) |

**관계 단위** (`{{#relations}}` 안)

| 변수 | 예시 |
|---|---|
| `fieldName`, `target`, `required` | `category`, `Category`, `false` |
| `capitalizedFieldName` | `Category` |
| `targetVarName`, `targetPackageName` | `category`, `category` |
| `joinColumnName` | `category_id` |
| `ownerName` | `Todo` (관계를 가진 엔티티) |
| `targetLabel` | `카테고리` |

**인자 단위** (`{{#params}}` 안): 필드와 관계를 같은 모양으로 합친 목록입니다. 생성자, Request, Response에서 씁니다.

| 변수 | 필드일 때 | 관계일 때 |
|---|---|---|
| `name` | `dueDate` | `category` |
| `javaType` | `LocalDate` | `Category` |
| `dtoName` | `dueDate` | `categoryId` |
| `dtoType` | `LocalDate` | `Long` |
| `label` | `마감일` | `카테고리` |
| `required` | `false` | `false` |
| `isRelation` | `false` | `true` |
| `validationAnnotation` | `@NotNull ` 또는 빈 문자열 | `@NotNull ` 또는 빈 문자열 |
| `responseExpression` | `entity.getDueDate()` | `entity.getCategory() != null ? entity.getCategory().getId() : null` |

렌더러는 `entities`, `fields`, `relations`, `params`의 모든 항목에 `first`, `last`(첫 번째·마지막 항목이면 `true`)를 붙입니다. 쉼표나 트리 기호(`├`, `└`) 처리에 씁니다.
