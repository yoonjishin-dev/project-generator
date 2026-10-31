package com.example.agent;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;

/**
 * 스키마 이름별로 미리 정한 응답을 순서대로 돌려준다. 받은 요청은 {@link #calls()}에 남긴다.
 */
public class FakeLlmClient implements LlmClient {

    public record Call(String systemPrompt, List<ChatTurn> conversation, String schemaName) {
    }

    private final Map<String, Deque<String>> responses;
    private final List<Call> calls = new ArrayList<>();

    public FakeLlmClient(Map<String, List<String>> responsesBySchema) {
        this.responses = new java.util.HashMap<>();
        responsesBySchema.forEach((schema, list) -> responses.put(schema, new ArrayDeque<>(list)));
    }

    @Override
    public String complete(String systemPrompt, List<ChatTurn> conversation, String schemaName, String schema) {
        calls.add(new Call(systemPrompt, List.copyOf(conversation), schemaName));
        Deque<String> queue = responses.get(schemaName);
        if (queue == null || queue.isEmpty()) {
            throw new IllegalStateException("준비된 응답이 없습니다: " + schemaName);
        }
        return queue.size() == 1 ? queue.peek() : queue.poll();
    }

    public List<Call> calls() {
        return calls;
    }
}
