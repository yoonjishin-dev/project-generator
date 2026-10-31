package com.example.pipeline;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * 실제 LLM(기본값: 로컬 Ollama)으로 전체 파이프라인을 돌린다. 몇 분 걸리므로 직접 켤 때만 실행한다.
 *
 * <pre>GENERATOR_LLM_IT=true ./gradlew test --tests '*LlmIntegrationTest' -i</pre>
 *
 * 결과는 {@code build/llm-it/todo-service}에 남는다.
 */
@EnabledIfEnvironmentVariable(named = "GENERATOR_LLM_IT", matches = "true")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, properties = "generator.output-dir=build/llm-it")
class LlmIntegrationTest {

    private static final Path OUTPUT = Path.of("build/llm-it/todo-service");

    @Autowired
    GenerationPipeline pipeline;

    @Test
    void 실제_LLM으로_요구사항에서_프로젝트를_만든다() throws IOException {
        deleteRecursively(OUTPUT);
        String requirements = new ObjectMapper()
                .readTree(Path.of("project-templates/examples/todo-service/input.json").toFile())
                .get("requirements").asText();

        GenerationResult result = pipeline.generate("todo-service", requirements);

        assertThat(result.projectDir().resolve("build.gradle.kts")).exists();
        assertThat(result.projectDir().resolve("docs/PRD.md")).exists();
        assertThat(result.spec().architecture().entities()).isNotEmpty();
    }

    private static void deleteRecursively(Path dir) throws IOException {
        if (!Files.exists(dir)) {
            return;
        }
        try (Stream<Path> paths = Files.walk(dir)) {
            for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                Files.delete(path);
            }
        }
    }
}
