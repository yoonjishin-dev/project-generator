package com.example.spec;

import java.util.regex.Pattern;

/**
 * 프로젝트명 규칙 ({@code rules.md} "네이밍"). 생성 경로와 패키지명에 그대로 쓰이므로 엄격하게 검사한다.
 */
public final class ProjectName {

    private static final Pattern PATTERN = Pattern.compile("[a-z][a-z0-9]*(-[a-z0-9]+)*");

    private ProjectName() {
    }

    public static String requireValid(String projectName) {
        if (projectName == null || !PATTERN.matcher(projectName).matches()) {
            throw new IllegalArgumentException("프로젝트명은 kebab-case여야 합니다: " + projectName);
        }
        return projectName;
    }
}
