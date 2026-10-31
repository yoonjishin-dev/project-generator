# 할 일 관리 서비스 API 명세

> project-generator 템플릿 `spring-boot` v1.0.0로 생성된 문서입니다.

## 공통

- Base URL: `http://localhost:8080`
- 요청과 응답 본문은 JSON(`Content-Type: application/json`)입니다.
- 날짜는 `2026-01-31`, 날짜와 시간은 `2026-01-31T09:30:00` 형식입니다.
- `PUT`은 전체 수정입니다. 요청에서 빠진 필드는 `null`로 저장됩니다.
- 오류는 [RFC 9457 ProblemDetail](https://www.rfc-editor.org/rfc/rfc9457) 형식으로 응답합니다.

```json
{
  "type": "about:blank",
  "title": "Not Found",
  "status": 404,
  "detail": "할 일을(를) 찾을 수 없습니다. id=1",
  "instance": "/api/todos/1"
}
```

| 상태 코드 | 의미 |
|---|---|
| `200` | 성공 |
| `201` | 생성 성공 |
| `204` | 삭제 성공 (본문 없음) |
| `400` | 요청 값 검증 실패 |
| `404` | 해당 ID가 없음 |
| `409` | 고유 값 중복, 또는 다른 데이터가 참조 중이라 삭제 불가 |

## 카테고리 (`/api/categories`)

| 메서드 | 경로 | 설명 | 요청 본문 | 성공 응답 |
|---|---|---|---|---|
| `GET` | `/api/categories` | 카테고리 목록 조회 | - | `200` CategoryResponse[] |
| `POST` | `/api/categories` | 카테고리 등록 | CategoryRequest | `201` CategoryResponse |
| `DELETE` | `/api/categories/{id}` | 카테고리 삭제 | - | `204` |

### CategoryRequest

| 필드 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `name` | String | O | 이름 |

### CategoryResponse

| 필드 | 타입 | 설명 |
|---|---|---|
| `id` | Long | 식별자 |
| `name` | String | 이름 |
| `createdAt` | LocalDateTime | 생성 시각 |
| `updatedAt` | LocalDateTime | 수정 시각 |

## 할 일 (`/api/todos`)

| 메서드 | 경로 | 설명 | 요청 본문 | 성공 응답 |
|---|---|---|---|---|
| `GET` | `/api/todos` | 할 일 목록 조회 | - | `200` TodoResponse[] |
| `GET` | `/api/todos/{id}` | 할 일 상세 조회 | - | `200` TodoResponse |
| `POST` | `/api/todos` | 할 일 등록 | TodoRequest | `201` TodoResponse |
| `PUT` | `/api/todos/{id}` | 할 일 수정 | TodoRequest | `200` TodoResponse |
| `DELETE` | `/api/todos/{id}` | 할 일 삭제 | - | `204` |

### TodoRequest

| 필드 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `title` | String | O | 제목 |
| `memo` | String | - | 메모 |
| `dueDate` | LocalDate | - | 마감일 |
| `completed` | Boolean | O | 완료 여부 |
| `categoryId` | Long | - | 카테고리 |

### TodoResponse

| 필드 | 타입 | 설명 |
|---|---|---|
| `id` | Long | 식별자 |
| `title` | String | 제목 |
| `memo` | String | 메모 |
| `dueDate` | LocalDate | 마감일 |
| `completed` | Boolean | 완료 여부 |
| `categoryId` | Long | 카테고리 |
| `createdAt` | LocalDateTime | 생성 시각 |
| `updatedAt` | LocalDateTime | 수정 시각 |
