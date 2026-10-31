package com.example.render;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * 템플릿 디렉토리의 {@code template.json}.
 */
public record TemplateManifest(
        String name,
        String description,
        List<FileEntry> files,
        @JsonProperty("static") List<StaticEntry> staticFiles
) {

    /** 렌더링하는 파일. {@code target}도 Mustache 템플릿이다. */
    public record FileEntry(String source, String target, Scope scope, String condition) {
    }

    /** 렌더링 없이 그대로 복사하는 파일. */
    public record StaticEntry(String source, String target, boolean executable) {
    }

    public enum Scope {
        @JsonProperty("project") PROJECT,
        @JsonProperty("entity") ENTITY
    }
}
