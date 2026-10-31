package com.example.input.web;

import com.example.job.GenerationJob;
import com.example.pipeline.GenerationResult;
import com.example.pipeline.GenerationStep;
import com.example.spec.ApiSpec;
import com.example.spec.Architecture;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 생성 작업 상태. 화면이 그대로 그릴 수 있도록 단계별 진행 상태와 결과 요약을 담는다.
 */
public record GenerationResponse(
        String id,
        String projectName,
        GenerationJob.Status status,
        List<Step> steps,
        Instant createdAt,
        Instant startedAt,
        Instant finishedAt,
        Result result,
        Error error
) {

    public enum StepState {
        DONE, CURRENT, PENDING, FAILED
    }

    public record Step(GenerationStep name, String label, StepState state) {
    }

    public record Result(String projectDir, int fileCount, String title, String summary, List<Entity> entities) {
    }

    public record Entity(String name, String label, String path, List<ApiSpec.Operation> operations) {
    }

    public record Error(String message, List<String> details) {
    }

    public static GenerationResponse from(GenerationJob.Snapshot job) {
        return new GenerationResponse(job.id(), job.projectName(), job.status(), steps(job), job.createdAt(),
                job.startedAt(), job.finishedAt(), result(job.result()),
                job.errorMessage() == null ? null : new Error(job.errorMessage(), job.errorDetails()));
    }

    private static List<Step> steps(GenerationJob.Snapshot job) {
        int current = job.step() == null ? -1 : job.step().ordinal();
        return Arrays.stream(GenerationStep.values())
                .map(step -> new Step(step, step.label(), state(job.status(), step.ordinal(), current)))
                .toList();
    }

    private static StepState state(GenerationJob.Status status, int index, int current) {
        return switch (status) {
            case SUCCEEDED -> StepState.DONE;
            case QUEUED -> StepState.PENDING;
            case RUNNING -> index < current ? StepState.DONE : index == current ? StepState.CURRENT : StepState.PENDING;
            case FAILED -> index < current ? StepState.DONE : index == current ? StepState.FAILED : StepState.PENDING;
        };
    }

    private static Result result(GenerationResult result) {
        if (result == null) {
            return null;
        }
        Map<String, ApiSpec.ResourceSpec> resources = result.spec().api().resources().stream()
                .collect(Collectors.toMap(ApiSpec.ResourceSpec::entity, Function.identity()));
        List<Entity> entities = result.spec().architecture().entities().stream()
                .map(entity -> entity(entity, resources.get(entity.name())))
                .toList();
        return new Result(result.projectDir().toString(), result.fileCount(), result.spec().prd().title(),
                result.spec().prd().summary(), entities);
    }

    private static Entity entity(Architecture.EntitySpec entity, ApiSpec.ResourceSpec resource) {
        return new Entity(entity.name(), entity.label(), resource.path(), resource.operations());
    }
}
