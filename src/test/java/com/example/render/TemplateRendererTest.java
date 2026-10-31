package com.example.render;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.spec.ApiSpec;
import com.example.spec.Architecture;
import com.example.spec.Prd;
import com.example.spec.ProjectSpec;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class TemplateRendererTest {

    private static final Path TEMPLATES = Path.of("project-templates");
    private static final Path TEMPLATE_DIR = TEMPLATES.resolve("templates/spring-boot");
    private static final Path EXAMPLE = TEMPLATES.resolve("examples/todo-service");
    private static final String VERSION = "v1.0.0";
    private static final Set<String> STATIC_FILES = Set.of(
            "gradlew", "gradlew.bat", "gradle/wrapper/gradle-wrapper.jar", "gradle/wrapper/gradle-wrapper.properties");

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final TemplateRenderer renderer = new TemplateRenderer(objectMapper, new RenderContextFactory(objectMapper));

    @Test
    void 예시_입력으로_렌더링하면_expected와_한_글자도_다르지_않다() throws IOException {
        Map<String, String> actual = asText(renderer.render(TEMPLATE_DIR, VERSION, exampleSpec()));
        Map<String, String> expected = readExpected(EXAMPLE.resolve("expected"));

        assertThat(actual.keySet()).containsAll(expected.keySet());
        assertThat(actual.keySet()).filteredOn(path -> !expected.containsKey(path))
                .containsExactlyInAnyOrderElementsOf(STATIC_FILES);
        expected.forEach((path, content) -> assertThat(actual.get(path)).as(path).isEqualTo(content));
    }

    @Test
    void 같은_입력이면_항상_같은_결과가_나온다() throws IOException {
        ProjectSpec spec = exampleSpec();

        assertThat(asText(renderer.render(TEMPLATE_DIR, VERSION, spec)))
                .isEqualTo(asText(renderer.render(TEMPLATE_DIR, VERSION, spec)));
    }

    @Test
    void gradlew는_실행_파일로_만든다() throws IOException {
        List<GeneratedFile> files = renderer.render(TEMPLATE_DIR, VERSION, exampleSpec());

        assertThat(files).filteredOn(GeneratedFile::executable)
                .extracting(GeneratedFile::path)
                .containsExactly("gradlew");
    }

    @Test
    void 프로젝트명이_kebab_case가_아니면_거부한다() throws IOException {
        ProjectSpec example = exampleSpec();
        ProjectSpec spec = new ProjectSpec("../Todo Service", example.prd(), example.architecture(), example.api());

        assertThatThrownBy(() -> renderer.render(TEMPLATE_DIR, VERSION, spec))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("kebab-case");
    }

    @Test
    void 엔티티_관계에_순환이_있으면_거부한다() throws IOException {
        ProjectSpec example = exampleSpec();
        List<Architecture.EntitySpec> cyclic = example.architecture().entities().stream()
                .map(entity -> entity.name().equals("Category")
                        ? new Architecture.EntitySpec(entity.name(), entity.label(), entity.description(),
                        entity.featureIds(), entity.fields(), List.of(new Architecture.RelationSpec(
                        Architecture.RelationType.MANY_TO_ONE, "todo", "Todo", false)))
                        : entity)
                .toList();
        ProjectSpec spec = new ProjectSpec(example.projectName(), example.prd(), new Architecture(cyclic), example.api());

        assertThatThrownBy(() -> renderer.render(TEMPLATE_DIR, VERSION, spec))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("순환");
    }

    private ProjectSpec exampleSpec() throws IOException {
        String projectName = objectMapper.readTree(EXAMPLE.resolve("input.json").toFile()).get("projectName").asText();
        return new ProjectSpec(
                projectName,
                objectMapper.readValue(EXAMPLE.resolve("prd.json").toFile(), Prd.class),
                objectMapper.readValue(EXAMPLE.resolve("architecture.json").toFile(), Architecture.class),
                objectMapper.readValue(EXAMPLE.resolve("api.json").toFile(), ApiSpec.class));
    }

    private static Map<String, String> asText(List<GeneratedFile> files) {
        return files.stream().collect(Collectors.toMap(GeneratedFile::path,
                file -> new String(file.content(), StandardCharsets.UTF_8), (a, b) -> a, TreeMap::new));
    }

    private static Map<String, String> readExpected(Path root) throws IOException {
        try (Stream<Path> paths = Files.walk(root)) {
            Map<String, String> files = new TreeMap<>();
            for (Path path : paths.filter(Files::isRegularFile).toList()) {
                files.put(root.relativize(path).toString().replace('\\', '/'), Files.readString(path));
            }
            return files;
        }
    }
}
