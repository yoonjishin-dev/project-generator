package com.example.pipeline;

import com.example.agent.ApiAgent;
import com.example.agent.ArchitectAgent;
import com.example.agent.PlannerAgent;
import com.example.config.GeneratorProperties;
import com.example.output.ProjectWriter;
import com.example.render.GeneratedFile;
import com.example.render.TemplateRenderer;
import com.example.spec.ApiSpec;
import com.example.spec.Architecture;
import com.example.spec.Prd;
import com.example.spec.ProjectName;
import com.example.spec.ProjectSpec;
import com.example.template.TemplateCheckout;
import com.example.template.TemplateRepository;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 요구사항 → PlannerAgent → ArchitectAgent → ApiAgent → 렌더링 → 로컬 생성 ({@code agents.md} "파이프라인").
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GenerationPipeline {

    public static final int MAX_REQUIREMENTS_LENGTH = 10_000;

    private final PlannerAgent plannerAgent;
    private final ArchitectAgent architectAgent;
    private final ApiAgent apiAgent;
    private final TemplateRenderer renderer;
    private final ProjectWriter writer;
    private final TemplateRepository templateRepository;
    private final GeneratorProperties properties;

    public GenerationResult generate(String projectName, String requirements) throws IOException {
        return generate(projectName, requirements, step -> {
        });
    }

    /**
     * @param onStep 각 단계를 시작할 때 호출된다. 화면에 진행 상황을 보여주는 용도
     */
    public GenerationResult generate(String projectName, String requirements, Consumer<GenerationStep> onStep)
            throws IOException {
        validate(projectName, requirements);
        Path projectDir = projectDir(projectName);
        // 에이전트 호출은 수십 초 이상 걸리므로, 생성할 수 없는 위치면 그 전에 멈춘다
        writer.ensureWritable(projectDir);

        // 템플릿을 받을 수 없으면 에이전트를 부르기 전에 멈춘다
        TemplateCheckout templates = templateRepository.checkout();
        log.info("[{}] 생성 시작 (템플릿 {} {})", projectName, properties.template(), templates);
        onStep.accept(GenerationStep.PLANNING);
        Prd prd = plannerAgent.plan(projectName, requirements);
        onStep.accept(GenerationStep.ARCHITECTURE);
        Architecture architecture = architectAgent.design(prd);
        onStep.accept(GenerationStep.API);
        ApiSpec api = apiAgent.design(prd, architecture);
        ProjectSpec spec = new ProjectSpec(projectName, prd, architecture, api);

        onStep.accept(GenerationStep.RENDERING);
        List<GeneratedFile> files = renderer.render(templateRepository.templateDir(), templates.version(), spec);
        onStep.accept(GenerationStep.WRITING);
        Path written = writer.write(projectDir, files);
        log.info("[{}] 생성 완료: {} (파일 {}개)", projectName, written, files.size());
        return new GenerationResult(written, spec, files.size());
    }

    /** 입력 검사와 생성 위치 확인만 한다. 작업을 접수하기 전에 바로 오류를 알려주는 용도. */
    public void precheck(String projectName, String requirements) throws IOException {
        validate(projectName, requirements);
        writer.ensureWritable(projectDir(projectName));
    }

    private Path projectDir(String projectName) {
        return properties.outputDir().resolve(projectName);
    }

    private static void validate(String projectName, String requirements) {
        ProjectName.requireValid(projectName);
        if (requirements == null || requirements.isBlank()) {
            throw new IllegalArgumentException("요구사항을 입력하세요");
        }
        if (requirements.length() > MAX_REQUIREMENTS_LENGTH) {
            throw new IllegalArgumentException("요구사항은 " + MAX_REQUIREMENTS_LENGTH + "자 이하로 입력하세요");
        }
    }
}
