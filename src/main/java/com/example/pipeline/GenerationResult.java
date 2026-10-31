package com.example.pipeline;

import com.example.spec.ProjectSpec;
import java.nio.file.Path;

/**
 * 생성된 프로젝트 위치, 생성에 쓴 에이전트 출력, 생성된 파일 수.
 */
public record GenerationResult(Path projectDir, ProjectSpec spec, int fileCount) {
}
