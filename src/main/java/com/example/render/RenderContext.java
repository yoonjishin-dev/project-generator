package com.example.render;

import java.util.List;
import java.util.Map;

/**
 * 템플릿에 넘기는 값. {@code project}는 모든 파일에서, {@code entities}의 각 항목은 엔티티 단위 파일에서 쓴다.
 */
public record RenderContext(Map<String, Object> project, List<Map<String, Object>> entities) {
}
