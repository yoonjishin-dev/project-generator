package com.example.agent;

import com.example.config.GeneratorProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 서브에이전트 공통 실행 흐름 ({@code agents.md} "공통 규칙", "실패 처리").
 * LLM 호출 → JSON 파싱 → 스키마 검증 → 교차 검증. 실패하면 직전 출력과 오류 목록을 붙여 다시 요청한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AgentRunner {

    // 작은 모델은 지시를 어기고 ```json ... ``` 으로 감싸는 경우가 많아서 벗겨 준다
    private static final Pattern CODE_FENCE = Pattern.compile("^\\s*```[a-zA-Z]*\\s*\\n(.*?)\\n\\s*```\\s*$", Pattern.DOTALL);

    private final LlmClient llmClient;
    private final TemplateFiles templateFiles;
    private final SchemaValidator schemaValidator;
    private final ObjectMapper objectMapper;
    private final GeneratorProperties properties;

    /**
     * @param agent      로그와 오류에 쓰는 에이전트 이름
     * @param skill      {@code skills/<skill>.md}
     * @param schemaName {@code schemas/<schemaName>.schema.json}
     * @param input      사용자 메시지로 보낼 입력. JSON으로 직렬화한다
     * @param type       출력을 변환할 타입
     * @param crossCheck 스키마 통과 후 실행하는 교차 검증. 오류 목록을 반환한다
     */
    public <T> T run(String agent, String skill, String schemaName, Object input, Class<T> type,
                     Function<T, List<String>> crossCheck) {
        String systemPrompt = templateFiles.systemPrompt(skill, schemaName);
        String schema = templateFiles.schema(schemaName);
        List<ChatTurn> conversation = new ArrayList<>(List.of(ChatTurn.user(toJson(input))));
        int attempts = properties.maxRetries() + 1;
        List<String> errors = List.of();

        for (int attempt = 1; attempt <= attempts; attempt++) {
            long started = System.currentTimeMillis();
            String output = llmClient.complete(systemPrompt, conversation, schemaName, schema);
            Result<T> result = check(output, schemaName, type, crossCheck);
            long elapsed = System.currentTimeMillis() - started;
            if (result.errors().isEmpty()) {
                log.info("{} 완료 ({}/{}번째 시도, {}ms)", agent, attempt, attempts, elapsed);
                return result.value();
            }
            errors = result.errors();
            log.warn("{} 출력 검증 실패 ({}/{}번째 시도, {}ms): {}", agent, attempt, attempts, elapsed, errors);
            conversation.add(ChatTurn.assistant(output));
            conversation.add(ChatTurn.user(retryMessage(errors)));
        }
        throw new AgentFailedException(agent, attempts, errors);
    }

    private <T> Result<T> check(String output, String schemaName, Class<T> type, Function<T, List<String>> crossCheck) {
        JsonNode json;
        try {
            json = objectMapper.readTree(stripCodeFence(output));
        } catch (JsonProcessingException e) {
            return Result.failure("출력이 올바른 JSON이 아닙니다: " + e.getOriginalMessage());
        }
        if (json == null || !json.isObject()) {
            return Result.failure("출력은 JSON 객체 하나여야 합니다.");
        }
        List<String> schemaErrors = schemaValidator.validate(schemaName, json);
        if (!schemaErrors.isEmpty()) {
            return new Result<>(null, schemaErrors);
        }
        T value;
        try {
            value = objectMapper.treeToValue(json, type);
        } catch (JsonProcessingException e) {
            return Result.failure("출력을 읽을 수 없습니다: " + e.getOriginalMessage());
        }
        return new Result<>(value, crossCheck.apply(value));
    }

    private static String stripCodeFence(String output) {
        if (output == null) {
            return "";
        }
        Matcher matcher = CODE_FENCE.matcher(output);
        return matcher.matches() ? matcher.group(1) : output;
    }

    private static String retryMessage(List<String> errors) {
        return "직전 출력에 아래 오류가 있습니다. 오류를 모두 고친 JSON 전체를 다시 출력하세요. 설명은 붙이지 마세요.\n- "
                + String.join("\n- ", errors);
    }

    private String toJson(Object input) {
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(input);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("에이전트 입력을 JSON으로 바꿀 수 없습니다", e);
        }
    }

    private record Result<T>(T value, List<String> errors) {

        static <T> Result<T> failure(String error) {
            return new Result<>(null, List.of(error));
        }
    }
}
