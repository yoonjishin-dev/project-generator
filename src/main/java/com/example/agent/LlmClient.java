package com.example.agent;

import java.util.List;

/**
 * LLM 호출. 테스트에서는 가짜 구현으로 바꿔 끼운다.
 */
public interface LlmClient {

    /**
     * @param systemPrompt 시스템 프롬프트
     * @param conversation 사용자 입력부터 시작하는 대화
     * @param schemaName   출력 JSON Schema 이름 (영문, 숫자, {@code _}, {@code -})
     * @param schema       출력 JSON Schema 본문. 모델이 지원하면 structured output으로 강제한다
     * @return 모델이 출력한 텍스트
     */
    String complete(String systemPrompt, List<ChatTurn> conversation, String schemaName, String schema);
}
