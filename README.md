# project-generator

요구사항과 프로젝트명을 입력하면, git에 저장된 템플릿과 가이드라인을 바탕으로 AI 에이전트가 기획서(PRD), 아키텍처, API 명세를 구조화하고 일관된 프로젝트를 로컬에 생성합니다.

## 기술 스택

- Java 21, Spring Boot 3.5
- Spring AI 1.1 (OpenAI 호환 API: Ollama, Gemini 등)
- MySQL, Spring Data JPA
- JGit (템플릿 저장소 관리)

## 사전 준비

### 1. JDK 21

### 2. MySQL

```sql
CREATE DATABASE project_generator;
```

### 3. Ollama (로컬 AI, 무료)

```bash
brew install ollama
ollama serve
ollama pull qwen2.5-coder:7b
```

## 실행

```bash
DB_PASSWORD=비밀번호 ./gradlew bootRun
```

## 환경변수

| 이름 | 기본값 | 설명 |
|---|---|---|
| `DB_HOST` | `localhost` | MySQL 호스트 |
| `DB_PORT` | `3306` | MySQL 포트 |
| `DB_NAME` | `project_generator` | 데이터베이스 이름 |
| `DB_USERNAME` | `root` | MySQL 사용자 |
| `DB_PASSWORD` | (없음) | MySQL 비밀번호 |
| `AI_BASE_URL` | `http://localhost:11434` | OpenAI 호환 API 주소 |
| `AI_API_KEY` | `ollama` | API 키 (Ollama는 검사하지 않음) |
| `AI_MODEL` | `qwen2.5-coder:7b` | 사용할 모델 |
