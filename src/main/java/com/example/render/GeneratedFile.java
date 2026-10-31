package com.example.render;

/**
 * 렌더링된 파일 하나. {@code path}는 생성될 프로젝트 루트 기준 상대 경로.
 */
public record GeneratedFile(String path, byte[] content, boolean executable) {
}
