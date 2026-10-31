package com.example.job;

import com.example.agent.AgentFailedException;
import com.example.pipeline.GenerationPipeline;
import com.example.pipeline.GenerationResult;
import java.io.IOException;
import java.net.ConnectException;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 생성 요청을 받아 순서대로 하나씩 실행한다. 로컬 LLM은 동시에 여러 요청을 받으면 모두 느려지기 때문이다.
 * 작업 목록은 메모리에만 있어서 앱을 다시 시작하면 사라진다(생성된 프로젝트 파일은 남는다).
 */
@Slf4j
@Service
public class GenerationJobService {

    private static final int RECENT_LIMIT = 20;

    private final GenerationPipeline pipeline;
    private final Executor executor;
    private final Map<String, GenerationJob> jobs = new ConcurrentHashMap<>();

    @Autowired
    public GenerationJobService(GenerationPipeline pipeline) {
        this(pipeline, Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, "generation");
            thread.setDaemon(true);
            return thread;
        }));
    }

    GenerationJobService(GenerationPipeline pipeline, Executor executor) {
        this.pipeline = pipeline;
        this.executor = executor;
    }

    /** 입력과 생성 위치를 먼저 확인하고 접수한다. 문제가 있으면 접수하지 않고 바로 예외. */
    public GenerationJob.Snapshot submit(String projectName, String requirements) throws IOException {
        return submit(projectName, requirements, job -> {
        });
    }

    /**
     * @param onFinish 성공하거나 실패해서 끝났을 때 작업 스레드에서 호출된다 (Slack 알림 등)
     */
    public synchronized GenerationJob.Snapshot submit(String projectName, String requirements,
                                                      Consumer<GenerationJob.Snapshot> onFinish) throws IOException {
        pipeline.precheck(projectName, requirements);
        boolean running = jobs.values().stream()
                .map(GenerationJob::snapshot)
                .anyMatch(job -> job.active() && job.projectName().equals(projectName));
        if (running) {
            throw new JobConflictException("'" + projectName + "' 프로젝트를 이미 생성하고 있습니다");
        }
        GenerationJob job = new GenerationJob(UUID.randomUUID().toString(), projectName, requirements);
        jobs.put(job.id(), job);
        GenerationJob.Snapshot submitted = job.snapshot();
        executor.execute(() -> run(job, onFinish));
        return submitted;
    }

    public Optional<GenerationJob.Snapshot> find(String id) {
        return Optional.ofNullable(jobs.get(id)).map(GenerationJob::snapshot);
    }

    /** 최근 작업부터. */
    public List<GenerationJob.Snapshot> recent() {
        return jobs.values().stream()
                .sorted(Comparator.comparing(GenerationJob::createdAt).reversed())
                .limit(RECENT_LIMIT)
                .map(GenerationJob::snapshot)
                .toList();
    }

    private void run(GenerationJob job, Consumer<GenerationJob.Snapshot> onFinish) {
        execute(job);
        try {
            onFinish.accept(job.snapshot());
        } catch (Exception e) {
            log.warn("[{}] 완료 알림 실패", job.projectName(), e);
        }
    }

    private void execute(GenerationJob job) {
        job.start();
        try {
            GenerationResult result = pipeline.generate(job.projectName(), job.requirements(), job::step);
            job.succeed(result);
        } catch (AgentFailedException e) {
            log.warn("[{}] 생성 실패: {}", job.projectName(), e.getMessage());
            job.fail(e.agent() + "의 출력이 재시도 후에도 검증을 통과하지 못했습니다.", e.errors());
        } catch (Exception e) {
            log.error("[{}] 생성 실패", job.projectName(), e);
            job.fail(describe(e), List.of());
        }
    }

    private static String describe(Exception e) {
        for (Throwable cause = e; cause != null; cause = cause.getCause()) {
            if (cause instanceof ConnectException) {
                return "AI 서버에 연결할 수 없습니다. Ollama가 실행 중인지(ollama serve), AI_BASE_URL이 맞는지 확인하세요.";
            }
        }
        return e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
    }
}
