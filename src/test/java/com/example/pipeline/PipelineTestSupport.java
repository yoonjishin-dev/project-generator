package com.example.pipeline;

import static com.example.agent.AgentTestSupport.OBJECT_MAPPER;
import static com.example.agent.AgentTestSupport.example;
import static com.example.agent.AgentTestSupport.properties;
import static com.example.agent.AgentTestSupport.runner;

import com.example.agent.AgentRunner;
import com.example.agent.ApiAgent;
import com.example.agent.ArchitectAgent;
import com.example.agent.CrossValidator;
import com.example.agent.FakeLlmClient;
import com.example.agent.PlannerAgent;
import com.example.config.GeneratorProperties;
import com.example.output.ProjectWriter;
import com.example.render.RenderContextFactory;
import com.example.render.TemplateRenderer;
import com.example.template.TemplateRepository;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * 가짜 LLM으로 실제 파이프라인을 조립한다.
 */
public final class PipelineTestSupport {

    private PipelineTestSupport() {
    }

    public static GenerationPipeline pipeline(FakeLlmClient llm, Path outputDir) {
        GeneratorProperties properties = properties(outputDir);
        AgentRunner runner = runner(llm, properties);
        CrossValidator crossValidator = new CrossValidator();
        return new GenerationPipeline(
                new PlannerAgent(runner, crossValidator),
                new ArchitectAgent(runner, crossValidator),
                new ApiAgent(runner, crossValidator),
                new TemplateRenderer(OBJECT_MAPPER, new RenderContextFactory(OBJECT_MAPPER)),
                new ProjectWriter(),
                new TemplateRepository(properties),
                properties);
    }

    /** 예시 JSON(examples/todo-service)을 그대로 돌려주는 LLM. */
    public static FakeLlmClient exampleLlm() {
        try {
            return new FakeLlmClient(Map.of(
                    "prd", List.of(example("prd.json")),
                    "architecture", List.of(example("architecture.json")),
                    "api", List.of(example("api.json"))));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static String exampleRequirements() {
        try {
            return OBJECT_MAPPER.readTree(example("input.json")).get("requirements").asText();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
