package com.example.template;

import java.nio.file.Path;

/**
 * 생성에 쓰는 템플릿 저장소 내용.
 *
 * @param root     agents.md, rules.md, skills/, schemas/, templates/가 있는 곳
 * @param version  템플릿 버전
 * @param commitId 저장소에서 받았을 때 태그가 가리키는 커밋. 폴더를 그대로 쓰면 null
 */
public record TemplateCheckout(Path root, String version, String commitId) {

    @Override
    public String toString() {
        return commitId == null
                ? version + " (폴더 " + root.toAbsolutePath() + ")"
                : version + " (" + commitId.substring(0, 7) + ", " + root + ")";
    }
}
