package com.example.agent;

import com.example.config.GeneratorProperties;
import com.example.template.TemplateRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Spring 없이 에이전트 구성 요소를 조립한다. 템플릿은 프로젝트 안의 {@code project-templates/}.
 */
public final class AgentTestSupport {

    public static final Path TEMPLATES = Path.of("project-templates");
    public static final Path EXAMPLE = TEMPLATES.resolve("examples/todo-service");
    public static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private AgentTestSupport() {
    }

    public static GeneratorProperties properties(Path outputDir) {
        return new GeneratorProperties(TEMPLATES, null, null, "spring-boot", "v1.0.0", outputDir, 2);
    }

    public static AgentRunner runner(LlmClient llmClient, GeneratorProperties properties) {
        TemplateFiles templateFiles = templateFiles(properties);
        return new AgentRunner(llmClient, templateFiles, new SchemaValidator(templateFiles, OBJECT_MAPPER),
                OBJECT_MAPPER, properties);
    }

    public static TemplateFiles templateFiles(GeneratorProperties properties) {
        return new TemplateFiles(new TemplateRepository(properties));
    }

    public static String example(String fileName) throws IOException {
        return Files.readString(EXAMPLE.resolve(fileName));
    }
}
