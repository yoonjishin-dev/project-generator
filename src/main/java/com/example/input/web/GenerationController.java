package com.example.input.web;

import com.example.job.GenerationJobService;
import java.io.IOException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

/**
 * 프로젝트 생성 요청 접수와 진행 상황 조회. 화면은 {@code static/index.html}.
 */
@RestController
@RequestMapping("/api/generations")
@RequiredArgsConstructor
public class GenerationController {

    private final GenerationJobService jobService;

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public GenerationResponse create(@RequestBody GenerationRequest request) throws IOException {
        return GenerationResponse.from(jobService.submit(request.projectName(), request.requirements()));
    }

    @GetMapping
    public List<GenerationResponse> list() {
        return jobService.recent().stream().map(GenerationResponse::from).toList();
    }

    @GetMapping("/{id}")
    public GenerationResponse get(@PathVariable String id) {
        return jobService.find(id)
                .map(GenerationResponse::from)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "생성 작업을 찾을 수 없습니다: " + id));
    }
}
