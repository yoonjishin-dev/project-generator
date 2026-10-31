package com.example.job;

import com.example.pipeline.GenerationResult;
import com.example.pipeline.GenerationStep;
import java.time.Instant;
import java.util.List;

/**
 * 생성 작업 하나. 작업 스레드가 상태를 바꾸고 웹 요청 스레드가 읽으므로, 읽을 때는 {@link #snapshot()}을 쓴다.
 */
public class GenerationJob {

    public enum Status {
        QUEUED, RUNNING, SUCCEEDED, FAILED
    }

    public record Snapshot(
            String id,
            String projectName,
            Status status,
            GenerationStep step,
            Instant createdAt,
            Instant startedAt,
            Instant finishedAt,
            GenerationResult result,
            String errorMessage,
            List<String> errorDetails
    ) {

        public boolean active() {
            return status == Status.QUEUED || status == Status.RUNNING;
        }
    }

    private final String id;
    private final String projectName;
    private final String requirements;
    private final Instant createdAt = Instant.now();

    private Status status = Status.QUEUED;
    private GenerationStep step;
    private Instant startedAt;
    private Instant finishedAt;
    private GenerationResult result;
    private String errorMessage;
    private List<String> errorDetails = List.of();

    public GenerationJob(String id, String projectName, String requirements) {
        this.id = id;
        this.projectName = projectName;
        this.requirements = requirements;
    }

    public String id() {
        return id;
    }

    public String projectName() {
        return projectName;
    }

    public String requirements() {
        return requirements;
    }

    public Instant createdAt() {
        return createdAt;
    }

    synchronized void start() {
        status = Status.RUNNING;
        startedAt = Instant.now();
    }

    synchronized void step(GenerationStep step) {
        this.step = step;
    }

    synchronized void succeed(GenerationResult result) {
        status = Status.SUCCEEDED;
        this.result = result;
        finishedAt = Instant.now();
    }

    synchronized void fail(String message, List<String> details) {
        status = Status.FAILED;
        errorMessage = message;
        errorDetails = List.copyOf(details);
        finishedAt = Instant.now();
    }

    public synchronized Snapshot snapshot() {
        return new Snapshot(id, projectName, status, step, createdAt, startedAt, finishedAt, result, errorMessage,
                errorDetails);
    }
}
