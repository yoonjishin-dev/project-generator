# 할 일 관리 서비스

개인 사용자가 할 일을 등록하고 카테고리별로 분류해 완료 여부와 마감일을 관리하는 서비스입니다.

> project-generator 템플릿 `spring-boot` v1.0.0로 생성된 프로젝트입니다.

## 문서

- [기획서 (PRD)](docs/PRD.md)
- [아키텍처](docs/architecture.md)
- [API 명세](docs/api-spec.md)

## 실행

```bash
# MySQL에 데이터베이스 생성
mysql -u root -p -e "CREATE DATABASE todo_service;"

DB_PASSWORD=비밀번호 ./gradlew bootRun
```

| 환경변수 | 기본값 |
|---|---|
| `DB_HOST` | `localhost` |
| `DB_PORT` | `3306` |
| `DB_NAME` | `todo_service` |
| `DB_USERNAME` | `root` |
| `DB_PASSWORD` | (없음) |
