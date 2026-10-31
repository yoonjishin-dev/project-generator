package com.example.config;

import java.nio.file.Path;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * {@code application.yml}의 {@code generator.*} 설정.
 *
 * @param templatesDir        템플릿 폴더 (agents.md, rules.md, skills/, schemas/, templates/가 있는 곳)
 * @param templatesRepository 템플릿을 별도 git 저장소로 분리했을 때만 설정 (URL 또는 로컬 경로).
 *                            설정하면 {@code templatesDir} 대신 그 저장소의 {@code templateVersion} 태그를 받아 쓴다
 * @param templatesCacheDir   저장소에서 받은 템플릿을 태그별로 보관하는 곳
 * @param template            사용할 템플릿 이름 ({@code templates/} 아래 디렉토리)
 * @param templateVersion     템플릿 버전. 생성 결과에 기록되고, 저장소를 쓸 때는 받을 태그가 된다
 * @param outputDir           프로젝트가 생성될 상위 디렉토리
 * @param maxRetries          에이전트 출력이 검증에 실패했을 때 다시 요청하는 횟수
 */
@ConfigurationProperties("generator")
public record GeneratorProperties(
        Path templatesDir,
        String templatesRepository,
        Path templatesCacheDir,
        String template,
        String templateVersion,
        Path outputDir,
        int maxRetries
) {

    public boolean useTemplatesRepository() {
        return templatesRepository != null && !templatesRepository.isBlank();
    }
}
