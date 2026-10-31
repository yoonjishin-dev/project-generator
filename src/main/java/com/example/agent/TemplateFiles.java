package com.example.agent;

import com.example.template.TemplateRepository;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 템플릿 저장소의 프롬프트와 스키마 파일을 읽는다.
 */
@Component
@RequiredArgsConstructor
public class TemplateFiles {

    private static final String COMMON_RULES_HEADING = "## 에이전트 공통 규칙";

    private final TemplateRepository templateRepository;

    /** 시스템 프롬프트 = {@code skills/<skill>.md} + {@code rules.md} "에이전트 공통 규칙" + 출력 스키마. */
    public String systemPrompt(String skill, String schemaName) {
        return read("skills/" + skill + ".md").strip()
                + "\n\n" + commonRules()
                + "\n\n## 출력 JSON Schema\n\n```json\n" + schema(schemaName).strip() + "\n```\n";
    }

    public String schema(String schemaName) {
        return read("schemas/" + schemaName + ".schema.json");
    }

    private String commonRules() {
        String rules = read("rules.md");
        int start = rules.indexOf(COMMON_RULES_HEADING);
        if (start < 0) {
            throw new IllegalStateException("rules.md에 '" + COMMON_RULES_HEADING + "' 섹션이 없습니다");
        }
        int end = rules.indexOf("\n## ", start + COMMON_RULES_HEADING.length());
        return (end < 0 ? rules.substring(start) : rules.substring(start, end)).strip();
    }

    private String read(String relativePath) {
        Path path = templateRepository.root().resolve(relativePath);
        try {
            return Files.readString(path, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("템플릿 저장소 파일을 읽을 수 없습니다: " + path, e);
        }
    }
}
