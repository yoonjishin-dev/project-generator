package com.example.pipeline;

/**
 * 파이프라인 진행 단계 ({@code agents.md} "파이프라인" 표의 순서).
 */
public enum GenerationStep {

    PLANNING("기획서(PRD) 작성"),
    ARCHITECTURE("아키텍처 설계"),
    API("API 설계"),
    RENDERING("템플릿 렌더링"),
    WRITING("파일 생성");

    private final String label;

    GenerationStep(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
