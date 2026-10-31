package com.example.agent;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SchemaValidatorsConfig;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/**
 * 에이전트 출력을 {@code schemas/*.schema.json}으로 검증한다.
 */
@Component
public class SchemaValidator {

    private final TemplateFiles templateFiles;
    private final ObjectMapper objectMapper;
    private final JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
    // 오류 메시지는 에이전트에게 그대로 전달된다. PC 언어 설정과 상관없이 같은 문구가 나오도록 고정한다
    private final SchemaValidatorsConfig config = SchemaValidatorsConfig.builder().locale(Locale.KOREAN).build();
    private final Map<String, JsonSchema> schemas = new ConcurrentHashMap<>();

    public SchemaValidator(TemplateFiles templateFiles, ObjectMapper objectMapper) {
        this.templateFiles = templateFiles;
        this.objectMapper = objectMapper;
    }

    /** 오류 메시지 목록. 비어 있으면 통과. 같은 입력이면 같은 순서로 나온다. */
    public List<String> validate(String schemaName, JsonNode json) {
        return schemas.computeIfAbsent(schemaName, this::load).validate(json).stream()
                .map(ValidationMessage::getMessage)
                .sorted()
                .toList();
    }

    private JsonSchema load(String schemaName) {
        try {
            return factory.getSchema(objectMapper.readTree(templateFiles.schema(schemaName)), config);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("스키마 파일이 올바른 JSON이 아닙니다: " + schemaName, e);
        }
    }
}
