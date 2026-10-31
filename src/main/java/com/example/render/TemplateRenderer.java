package com.example.render;

import com.example.render.TemplateManifest.FileEntry;
import com.example.render.TemplateManifest.StaticEntry;
import com.example.spec.ProjectSpec;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.samskivert.mustache.Mustache;
import com.samskivert.mustache.Template;
import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 템플릿 디렉토리와 에이전트 출력으로 프로젝트 파일 목록을 만든다. 파일은 쓰지 않는다({@code ProjectWriter}의 일).
 * 렌더링 규칙은 {@code project-templates/README.md}의 "템플릿 문법"과 같아야 한다.
 */
@Component
@RequiredArgsConstructor
public class TemplateRenderer {

    private static final String MANIFEST = "template.json";

    // 없는 변수를 쓰면 빈 문자열 대신 예외를 내서 템플릿 오타를 바로 드러낸다
    private static final Mustache.Compiler COMPILER = Mustache.compiler().escapeHTML(false);

    private final ObjectMapper objectMapper;
    private final RenderContextFactory contextFactory;

    public List<GeneratedFile> render(Path templateDir, String templateVersion, ProjectSpec spec) {
        TemplateManifest manifest = readManifest(templateDir);
        RenderContext context = contextFactory.create(spec, templateVersion);
        Map<String, Object> project = context.project();

        List<GeneratedFile> files = new ArrayList<>();
        for (FileEntry entry : manifest.files()) {
            Template body = COMPILER.compile(readString(templateDir.resolve(entry.source())));
            Template target = COMPILER.compile(entry.target());
            switch (entry.scope()) {
                case PROJECT -> files.add(textFile(target.execute(project), body.execute(project)));
                case ENTITY -> {
                    for (Map<String, Object> entity : context.entities()) {
                        if (entry.condition() != null && !Boolean.TRUE.equals(entity.get(entry.condition()))) {
                            continue;
                        }
                        files.add(textFile(execute(target, entity, project), execute(body, entity, project)));
                    }
                }
            }
        }
        for (StaticEntry entry : manifest.staticFiles()) {
            files.add(new GeneratedFile(entry.target(), readBytes(templateDir.resolve(entry.source())), entry.executable()));
        }
        return files;
    }

    private TemplateManifest readManifest(Path templateDir) {
        try {
            return objectMapper.readValue(templateDir.resolve(MANIFEST).toFile(), TemplateManifest.class);
        } catch (IOException e) {
            throw new UncheckedIOException("템플릿 정의를 읽을 수 없습니다: " + templateDir.resolve(MANIFEST), e);
        }
    }

    /** 엔티티 값을 먼저 찾고, 없으면 프로젝트 값을 찾는다. */
    private static String execute(Template template, Map<String, Object> entity, Map<String, Object> project) {
        StringWriter out = new StringWriter();
        template.execute(entity, project, out);
        return out.toString();
    }

    /** 줄바꿈은 {@code \n}, 줄 끝 공백 제거, 파일 끝 줄바꿈 하나. */
    private static GeneratedFile textFile(String path, String content) {
        String normalized = content.replace("\r\n", "\n").lines()
                .map(String::stripTrailing)
                .collect(Collectors.joining("\n"))
                .stripTrailing() + "\n";
        return new GeneratedFile(path, normalized.getBytes(StandardCharsets.UTF_8), false);
    }

    private static String readString(Path path) {
        try {
            return Files.readString(path, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("템플릿 파일을 읽을 수 없습니다: " + path, e);
        }
    }

    private static byte[] readBytes(Path path) {
        try {
            return Files.readAllBytes(path);
        } catch (IOException e) {
            throw new UncheckedIOException("파일을 읽을 수 없습니다: " + path, e);
        }
    }
}
