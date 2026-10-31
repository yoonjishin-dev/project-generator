package com.example.agent;

import java.util.List;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.ResponseFormat;
import org.springframework.stereotype.Component;

/**
 * Spring AI의 OpenAI 호환 클라이언트로 호출한다. 모델, temperature, seed는 {@code spring.ai.openai.chat.options.*}.
 */
@Component
public class SpringAiLlmClient implements LlmClient {

    private final ChatClient chatClient;

    public SpringAiLlmClient(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    @Override
    public String complete(String systemPrompt, List<ChatTurn> conversation, String schemaName, String schema) {
        List<Message> messages = conversation.stream()
                .map(turn -> switch (turn.role()) {
                    case USER -> (Message) new UserMessage(turn.content());
                    case ASSISTANT -> new AssistantMessage(turn.content());
                })
                .toList();
        // strict는 끈다: OpenAI strict 모드가 허용하지 않는 키워드(not 등)가 스키마에 있고, 최종 검증은 SchemaValidator가 한다
        ResponseFormat responseFormat = ResponseFormat.builder()
                .type(ResponseFormat.Type.JSON_SCHEMA)
                .jsonSchema(ResponseFormat.JsonSchema.builder().name(schemaName).schema(schema).strict(false).build())
                .build();
        return chatClient.prompt()
                .system(systemPrompt)
                .messages(messages)
                .options(OpenAiChatOptions.builder().responseFormat(responseFormat).build())
                .call()
                .content();
    }
}
