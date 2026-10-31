package com.example.input.slack;

import static com.example.agent.AgentTestSupport.OBJECT_MAPPER;
import static com.example.agent.AgentTestSupport.example;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.example.job.GenerationJob;
import com.example.job.GenerationJob.Snapshot;
import com.example.job.GenerationJob.Status;
import com.example.job.GenerationJobService;
import com.example.job.JobConflictException;
import com.example.pipeline.GenerationResult;
import com.example.pipeline.GenerationStep;
import com.example.spec.ApiSpec;
import com.example.spec.Architecture;
import com.example.spec.Prd;
import com.example.spec.ProjectSpec;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class SlackCommandHandlerTest {

    private static final String RESPONSE_URL = "https://hooks.slack.com/commands/T1/123/abc";
    private static final String WEB_URL = "http://localhost:8080";

    private final GenerationJobService jobService = mock(GenerationJobService.class);
    private final List<SlackMessage> sent = new ArrayList<>();
    private final SlackNotifier notifier = (url, message) -> sent.add(message);

    @Test
    void 프로젝트명과_요구사항을_나눠_접수하고_진행_링크를_알려준다() throws IOException {
        given(jobService.submit(eq("todo-service"), anyString(), any())).willReturn(snapshot(Status.QUEUED));

        String reply = handler(List.of()).handle(command("U1", "  todo-service   할 일을 등록하고\n카테고리로 분류  "));

        verify(jobService).submit(eq("todo-service"), eq("할 일을 등록하고\n카테고리로 분류"), any());
        assertThat(reply).contains("*todo-service* 생성 요청을 접수했습니다").contains(WEB_URL + "/?job=job-1");
    }

    @Test
    void Slack이_이스케이프한_글자를_되돌려서_넘긴다() throws IOException {
        given(jobService.submit(anyString(), anyString(), any())).willReturn(snapshot(Status.QUEUED));

        handler(List.of()).handle(command("U1", "shop-app 가격 &lt; 1000 &amp; 재고 &gt; 0 인 상품만"));

        verify(jobService).submit(eq("shop-app"), eq("가격 < 1000 & 재고 > 0 인 상품만"), any());
    }

    @Test
    void 비어_있거나_help면_사용법을_보여준다() throws IOException {
        SlackCommandHandler handler = handler(List.of());

        assertThat(handler.handle(command("U1", ""))).startsWith("사용법: `/generate <프로젝트명> <요구사항>`");
        assertThat(handler.handle(command("U1", "help"))).startsWith("사용법:");
        assertThat(handler.handle(command("U1", "todo-service"))).contains("요구사항을 입력하세요");
        verify(jobService, never()).submit(anyString(), anyString(), any());
    }

    @Test
    void 허용_목록이_있으면_목록에_없는_사용자는_거부한다() throws IOException {
        String reply = handler(List.of("U-ADMIN")).handle(command("U-OTHER", "todo-service 할 일 관리"));

        assertThat(reply).contains("권한이 없습니다");
        verify(jobService, never()).submit(anyString(), anyString(), any());
    }

    @Test
    void 접수할_수_없으면_이유와_사용법을_돌려준다() throws IOException {
        given(jobService.submit(anyString(), anyString(), any()))
                .willThrow(new IllegalArgumentException("프로젝트명은 kebab-case여야 합니다: Todo"))
                .willThrow(new FileAlreadyExistsException("/out/todo-service"))
                .willThrow(new JobConflictException("'todo-service' 프로젝트를 이미 생성하고 있습니다"));
        SlackCommandHandler handler = handler(List.of());

        assertThat(handler.handle(command("U1", "Todo 할 일"))).contains("kebab-case").contains("사용법:");
        assertThat(handler.handle(command("U1", "todo-service 할 일"))).contains("폴더가 이미 있습니다");
        assertThat(handler.handle(command("U1", "todo-service 할 일"))).contains("이미 생성하고 있습니다");
    }

    @Test
    void 끝나면_채널에_결과_요약을_보낸다() throws IOException {
        Consumer<Snapshot> onFinish = submitAndCaptureCallback();

        onFinish.accept(succeeded());

        SlackMessage message = sent.get(0);
        assertThat(message.inChannel()).isTrue();
        assertThat(message.text())
                .startsWith(":white_check_mark: <@U1> 님이 요청한 *todo-service* 생성이 끝났습니다 (2분 5초)")
                .contains("*할 일 관리 서비스* — 파일 28개")
                .contains("• 할 일 `/api/todos` (LIST, GET, CREATE, UPDATE, DELETE)")
                .contains("위치: `/out/todo-service`")
                .contains("<" + WEB_URL + "/?job=job-1|웹에서 보기>");
    }

    @Test
    void 실패하면_실패한_단계와_오류를_보낸다() throws IOException {
        Consumer<Snapshot> onFinish = submitAndCaptureCallback();
        List<String> details = List.of("a <b>", "2", "3", "4", "5", "6", "7");

        onFinish.accept(new Snapshot("job-1", "todo-service", Status.FAILED, GenerationStep.ARCHITECTURE,
                Instant.EPOCH, Instant.EPOCH, Instant.EPOCH.plusSeconds(40), null, "ArchitectAgent 실패", details));

        assertThat(sent.get(0).text())
                .startsWith(":x: <@U1> 님이 요청한 *todo-service* 생성에 실패했습니다 (아키텍처 설계 단계, 40초)")
                .contains("• a &lt;b&gt;")
                .contains("• 외 2개")
                .doesNotContain("• 6");
    }

    private Consumer<Snapshot> submitAndCaptureCallback() throws IOException {
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Consumer<Snapshot>> callback = ArgumentCaptor.forClass(Consumer.class);
        given(jobService.submit(anyString(), anyString(), callback.capture())).willReturn(snapshot(Status.QUEUED));
        handler(List.of()).handle(command("U1", "todo-service 할 일 관리"));
        return callback.getValue();
    }

    private SlackCommandHandler handler(List<String> allowedUserIds) {
        return new SlackCommandHandler(jobService, notifier,
                new SlackProperties("xoxb-test", "xapp-test", "/generate", allowedUserIds, WEB_URL));
    }

    private static SlackCommand command(String userId, String text) {
        return new SlackCommand(userId, text, RESPONSE_URL);
    }

    private static Snapshot snapshot(Status status) {
        return new Snapshot("job-1", "todo-service", status, null, Instant.EPOCH, null, null, null, null, List.of());
    }

    private static Snapshot succeeded() throws IOException {
        ProjectSpec spec = new ProjectSpec("todo-service",
                OBJECT_MAPPER.readValue(example("prd.json"), Prd.class),
                OBJECT_MAPPER.readValue(example("architecture.json"), Architecture.class),
                OBJECT_MAPPER.readValue(example("api.json"), ApiSpec.class));
        return new Snapshot("job-1", "todo-service", GenerationJob.Status.SUCCEEDED, GenerationStep.WRITING,
                Instant.EPOCH, Instant.EPOCH, Instant.EPOCH.plusSeconds(125),
                new GenerationResult(Path.of("/out/todo-service"), spec, 28), null, List.of());
    }
}
