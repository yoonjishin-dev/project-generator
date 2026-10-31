package com.example.spec;

/**
 * 프로젝트 하나를 렌더링하는 데 필요한 입력 전체. 에이전트 출력 JSON 3개와 프로젝트명.
 */
public record ProjectSpec(String projectName, Prd prd, Architecture architecture, ApiSpec api) {
}
