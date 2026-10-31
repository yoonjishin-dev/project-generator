package com.example.input.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.job.GenerationJob;
import com.example.job.GenerationJobService;
import com.example.job.JobConflictException;
import com.example.pipeline.GenerationStep;
import java.nio.file.FileAlreadyExistsException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(GenerationController.class)
class GenerationControllerTest {

    private static final String BODY = "{\"projectName\":\"todo-service\",\"requirements\":\"할 일 관리\"}";

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    GenerationJobService jobService;

    @Test
    void 접수하면_202와_작업_상태를_돌려준다() throws Exception {
        given(jobService.submit("todo-service", "할 일 관리")).willReturn(snapshot(GenerationJob.Status.QUEUED, null));

        mockMvc.perform(post("/api/generations").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.id").value("job-1"))
                .andExpect(jsonPath("$.status").value("QUEUED"))
                .andExpect(jsonPath("$.steps.length()").value(5))
                .andExpect(jsonPath("$.steps[0].label").value("기획서(PRD) 작성"))
                .andExpect(jsonPath("$.steps[0].state").value("PENDING"));
    }

    @Test
    void 진행_중이면_지난_단계는_DONE_지금_단계는_CURRENT() throws Exception {
        given(jobService.find("job-1")).willReturn(Optional.of(snapshot(GenerationJob.Status.RUNNING, GenerationStep.API)));

        mockMvc.perform(get("/api/generations/job-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.steps[*].state").value(org.hamcrest.Matchers.contains(
                        "DONE", "DONE", "CURRENT", "PENDING", "PENDING")));
    }

    @Test
    void 실패하면_실패한_단계와_오류를_돌려준다() throws Exception {
        GenerationJob.Snapshot failed = new GenerationJob.Snapshot("job-1", "todo-service", GenerationJob.Status.FAILED,
                GenerationStep.ARCHITECTURE, Instant.now(), Instant.now(), Instant.now(), null,
                "ArchitectAgent의 출력이 재시도 후에도 검증을 통과하지 못했습니다.", List.of("entities: 비어 있음"));
        given(jobService.find("job-1")).willReturn(Optional.of(failed));

        mockMvc.perform(get("/api/generations/job-1"))
                .andExpect(jsonPath("$.steps[*].state").value(org.hamcrest.Matchers.contains(
                        "DONE", "FAILED", "PENDING", "PENDING", "PENDING")))
                .andExpect(jsonPath("$.error.message").value(org.hamcrest.Matchers.containsString("ArchitectAgent")))
                .andExpect(jsonPath("$.error.details[0]").value("entities: 비어 있음"));
    }

    @Test
    void 잘못된_입력은_400_ProblemDetail() throws Exception {
        given(jobService.submit(any(), any())).willThrow(new IllegalArgumentException("프로젝트명은 kebab-case여야 합니다: Todo"));

        mockMvc.perform(post("/api/generations").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("프로젝트명은 kebab-case여야 합니다: Todo"));
    }

    @Test
    void 이미_있는_폴더나_진행_중인_작업은_409() throws Exception {
        given(jobService.submit(any(), any()))
                .willThrow(new FileAlreadyExistsException("/out/todo-service"))
                .willThrow(new JobConflictException("'todo-service' 프로젝트를 이미 생성하고 있습니다"));

        mockMvc.perform(post("/api/generations").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("/out/todo-service")));
        mockMvc.perform(post("/api/generations").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("이미 생성하고 있습니다")));
    }

    @Test
    void 없는_작업은_404() throws Exception {
        given(jobService.find("nope")).willReturn(Optional.empty());

        mockMvc.perform(get("/api/generations/nope")).andExpect(status().isNotFound());
    }

    private static GenerationJob.Snapshot snapshot(GenerationJob.Status status, GenerationStep step) {
        return new GenerationJob.Snapshot("job-1", "todo-service", status, step, Instant.now(), null, null, null, null,
                List.of());
    }
}
