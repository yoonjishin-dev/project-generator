package com.example.pipeline;

import static com.example.agent.AgentTestSupport.EXAMPLE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.agent.FakeLlmClient;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class GenerationPipelineTest {

    @TempDir
    Path outputDir;

    @Test
    void 요구사항부터_로컬_생성까지_expected와_같은_프로젝트를_만든다() throws IOException {
        FakeLlmClient llm = exampleLlm();

        GenerationResult result = pipeline(llm).generate("todo-service", requirements());

        assertThat(result.projectDir()).isEqualTo(outputDir.resolve("todo-service").toAbsolutePath());
        assertThat(llm.calls()).extracting(FakeLlmClient.Call::schemaName).containsExactly("prd", "architecture", "api");
        Path expected = EXAMPLE.resolve("expected");
        try (Stream<Path> paths = Files.walk(expected)) {
            for (Path file : paths.filter(Files::isRegularFile).toList()) {
                Path actual = result.projectDir().resolve(expected.relativize(file).toString());
                assertThat(actual).as(actual.toString()).hasSameBinaryContentAs(file);
            }
        }
        assertThat(Files.isExecutable(result.projectDir().resolve("gradlew"))).isTrue();
    }

    @Test
    void 앞_단계_출력이_다음_에이전트의_입력으로_넘어간다() throws IOException {
        FakeLlmClient llm = exampleLlm();

        pipeline(llm).generate("todo-service", requirements());

        assertThat(llm.calls().get(1).conversation().get(0).content()).contains("\"title\" : \"할 일 관리 서비스\"");
        assertThat(llm.calls().get(2).conversation().get(0).content())
                .contains("\"prd\" :").contains("\"architecture\" :").contains("\"name\" : \"Todo\"");
    }

    @Test
    void 이미_있는_프로젝트면_에이전트를_부르기_전에_멈춘다() throws IOException {
        Files.createDirectories(outputDir.resolve("todo-service"));
        Files.writeString(outputDir.resolve("todo-service/mine.txt"), "keep");
        FakeLlmClient llm = exampleLlm();

        assertThatThrownBy(() -> pipeline(llm).generate("todo-service", requirements()))
                .isInstanceOf(FileAlreadyExistsException.class);
        assertThat(llm.calls()).isEmpty();
    }

    @Test
    void 잘못된_프로젝트명이나_빈_요구사항은_거부한다() {
        FakeLlmClient llm = exampleLlm();

        assertThatThrownBy(() -> pipeline(llm).generate("Todo Service", requirements()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> pipeline(llm).generate("todo-service", "  "))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(llm.calls()).isEmpty();
    }

    private GenerationPipeline pipeline(FakeLlmClient llm) {
        return PipelineTestSupport.pipeline(llm, outputDir);
    }

    private static FakeLlmClient exampleLlm() {
        return PipelineTestSupport.exampleLlm();
    }

    private static String requirements() {
        return PipelineTestSupport.exampleRequirements();
    }

    @Test
    void 단계가_순서대로_알려진다() throws IOException {
        List<GenerationStep> steps = new java.util.ArrayList<>();

        pipeline(exampleLlm()).generate("todo-service", requirements(), steps::add);

        assertThat(steps).containsExactly(GenerationStep.values());
    }
}
