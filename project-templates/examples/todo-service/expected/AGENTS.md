# AGENTS.md

이 프로젝트에서 코드를 작성하는 사람과 AI 에이전트가 지켜야 할 규칙입니다.
project-generator 템플릿 `spring-boot` v1.0.0의 규칙을 따릅니다.

## 구조

- 기본 패키지: `com.generated.todoservice`
- 도메인별 패키지: `com.generated.todoservice.category`, `com.generated.todoservice.todo`
- 공통 코드: `com.generated.todoservice.common`
- 도메인 하나 = `Entity`, `Repository`, `Service`, `Controller`, `Request`(record), `Response`(record)

## 규칙

- 엔티티에는 `@Getter`, `@NoArgsConstructor(access = AccessLevel.PROTECTED)`만 씁니다. `@Setter`, `@Data`는 쓰지 않고 변경은 `update(...)` 메서드로 합니다.
- 엔티티를 컨트롤러 밖으로 직접 반환하지 않고 `Response` record로 변환합니다.
- Service 클래스에는 `@Transactional(readOnly = true)`, 쓰기 메서드에만 `@Transactional`을 붙입니다.
- 의존성은 생성자 주입(`@RequiredArgsConstructor` + `private final`)으로 받습니다.
- 응답 코드: 생성 `201`, 삭제 `204`, 검증 실패 `400`, 없는 ID `404`, 중복·참조 충돌 `409` (모두 ProblemDetail 형식).
- 와일드카드 import는 `jakarta.persistence.*`, `jakarta.validation.constraints.*`, `org.springframework.web.bind.annotation.*`에만 씁니다.
- 기능을 추가하면 `docs/`의 PRD, 아키텍처, API 명세도 함께 고칩니다.
