package com.example.job;

import static com.example.agent.AgentTestSupport.example;
import static com.example.pipeline.PipelineTestSupport.exampleLlm;
import static com.example.pipeline.PipelineTestSupport.exampleRequirements;
import static com.example.pipeline.PipelineTestSupport.pipeline;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.agent.FakeLlmClient;
import com.example.job.GenerationJob.Snapshot;
import com.example.job.GenerationJob.Status;
import com.example.pipeline.GenerationStep;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class GenerationJobServiceTest {

    @TempDir
    Path outputDir;

    /** 실행을 미뤄 두었다가 직접 돌린다. 접수 직후 상태(QUEUED)를 확인하기 위해서. */
    private final List<Runnable> queued = new ArrayList<>();

    @Test
    void 접수하면_대기_상태로_돌려주고_실행이_끝나면_성공한다() throws IOException {
        GenerationJobService service = new GenerationJobService(pipeline(exampleLlm(), outputDir), queued::add);

        Snapshot submitted = service.submit("todo-service", exampleRequirements());

        assertThat(submitted.status()).isEqualTo(Status.QUEUED);
        assertThat(submitted.active()).isTrue();
        queued.forEach(Runnable::run);

        Snapshot done = service.find(submitted.id()).orElseThrow();
        assertThat(done.status()).isEqualTo(Status.SUCCEEDED);
        assertThat(done.step()).isEqualTo(GenerationStep.WRITING);
        assertThat(done.result().fileCount()).isEqualTo(28);
        assertThat(done.startedAt()).isNotNull();
        assertThat(done.finishedAt()).isAfterOrEqualTo(done.startedAt());
        assertThat(outputDir.resolve("todo-service/build.gradle.kts")).exists();
    }

    @Test
    void 에이전트가_끝내_실패하면_어느_단계에서_왜_실패했는지_남긴다() throws IOException {
        FakeLlmClient llm = new FakeLlmClient(Map.of(
                "prd", List.of(example("prd.json")),
                "architecture", List.of("{\"entities\": []}")));
        GenerationJobService service = new GenerationJobService(pipeline(llm, outputDir), Runnable::run);

        Snapshot job = service.find(service.submit("todo-service", exampleRequirements()).id()).orElseThrow();

        assertThat(job.status()).isEqualTo(Status.FAILED);
        assertThat(job.step()).isEqualTo(GenerationStep.ARCHITECTURE);
        assertThat(job.errorMessage()).contains("ArchitectAgent");
        assertThat(job.errorDetails()).isNotEmpty();
        assertThat(outputDir.resolve("todo-service")).doesNotExist();
    }

    @Test
    void AI_서버에_연결할_수_없으면_확인할_것을_알려준다() throws IOException {
        FakeLlmClient llm = new FakeLlmClient(Map.of()) {
            @Override
            public String complete(String systemPrompt, List<com.example.agent.ChatTurn> conversation, String schemaName,
                                   String schema) {
                throw new IllegalStateException("I/O error", new java.net.ConnectException("Connection refused"));
            }
        };
        GenerationJobService service = new GenerationJobService(pipeline(llm, outputDir), Runnable::run);

        Snapshot job = service.find(service.submit("todo-service", exampleRequirements()).id()).orElseThrow();

        assertThat(job.status()).isEqualTo(Status.FAILED);
        assertThat(job.errorMessage()).contains("Ollama");
    }

    @Test
    void 끝나면_완료_콜백에_최종_상태를_넘긴다() throws IOException {
        GenerationJobService service = new GenerationJobService(pipeline(exampleLlm(), outputDir), Runnable::run);
        List<Snapshot> finished = new ArrayList<>();

        service.submit("todo-service", exampleRequirements(), finished::add);

        assertThat(finished).singleElement().extracting(Snapshot::status).isEqualTo(Status.SUCCEEDED);
    }

    @Test
    void 완료_콜백이_실패해도_작업_결과는_그대로다() throws IOException {
        GenerationJobService service = new GenerationJobService(pipeline(exampleLlm(), outputDir), Runnable::run);

        Snapshot job = service.submit("todo-service", exampleRequirements(), done -> {
            throw new IllegalStateException("알림 실패");
        });

        assertThat(service.find(job.id()).orElseThrow().status()).isEqualTo(Status.SUCCEEDED);
    }

    @Test
    void 같은_이름으로_진행_중인_작업이_있으면_거부한다() throws IOException {
        GenerationJobService service = new GenerationJobService(pipeline(exampleLlm(), outputDir), queued::add);
        service.submit("todo-service", exampleRequirements());

        assertThatThrownBy(() -> service.submit("todo-service", exampleRequirements()))
                .isInstanceOf(JobConflictException.class);
        assertThat(service.recent()).hasSize(1);
    }

    @Test
    void 이미_있는_폴더나_잘못된_입력은_접수하지_않는다() throws IOException {
        Files.createDirectories(outputDir.resolve("todo-service"));
        Files.writeString(outputDir.resolve("todo-service/mine.txt"), "keep");
        GenerationJobService service = new GenerationJobService(pipeline(exampleLlm(), outputDir), queued::add);

        assertThatThrownBy(() -> service.submit("todo-service", exampleRequirements()))
                .isInstanceOf(FileAlreadyExistsException.class);
        assertThatThrownBy(() -> service.submit("Todo", exampleRequirements()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.submit("todo-app", "x".repeat(10_001)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("10000자");
        assertThat(service.recent()).isEmpty();
        assertThat(queued).isEmpty();
    }

    @Test
    void 최근_작업이_먼저_나온다() throws IOException, InterruptedException {
        GenerationJobService service = new GenerationJobService(pipeline(exampleLlm(), outputDir), queued::add);
        service.submit("first-app", exampleRequirements());
        Thread.sleep(5);
        service.submit("second-app", exampleRequirements());

        assertThat(service.recent()).extracting(Snapshot::projectName).containsExactly("second-app", "first-app");
    }
}
