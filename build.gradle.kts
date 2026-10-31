plugins {
    java
    id("org.springframework.boot") version "3.5.6"
    id("io.spring.dependency-management") version "1.1.7"
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.ai:spring-ai-bom:1.1.8")
    }
}

group = "com.example"
version = "1.0.0"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    // Spring Boot
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")

    // MySQL
    runtimeOnly("com.mysql:mysql-connector-j")

    // OpenAI 호환 API 클라이언트 (Ollama, Gemini 등에 공통으로 사용)
    implementation("org.springframework.ai:spring-ai-starter-model-openai")

    // JGit
    implementation("org.eclipse.jgit:org.eclipse.jgit:7.8.0.202609011348-r")

    // JSON
    implementation("com.google.code.gson:gson")

    // Mustache 템플릿 렌더링
    implementation("com.samskivert:jmustache")

    // 에이전트 출력 JSON Schema 검증 (3.x는 Jackson 3 기반이라 Spring Boot 3.5와 맞지 않음)
    implementation("com.networknt:json-schema-validator:1.5.9")

    // Slack 입력 (Socket Mode: 공개 주소 없이 앱이 Slack에 먼저 접속한다)
    implementation("com.slack.api:bolt-socket-mode:1.53.0")
    implementation("org.java-websocket:Java-WebSocket:1.6.0")

    // Lombok
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")

    // Test
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
    // 템플릿이나 정답 결과(expected/)가 바뀌면 렌더러 테스트를 다시 돌린다
    inputs.dir("project-templates").withPathSensitivity(PathSensitivity.RELATIVE)
}
