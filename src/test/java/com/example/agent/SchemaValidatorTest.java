package com.example.agent;

import static com.example.agent.AgentTestSupport.OBJECT_MAPPER;
import static com.example.agent.AgentTestSupport.example;
import static com.example.agent.AgentTestSupport.properties;
import static com.example.agent.AgentTestSupport.templateFiles;
import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.nio.file.Path;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.api.Test;

class SchemaValidatorTest {

    private final SchemaValidator validator =
            new SchemaValidator(templateFiles(properties(Path.of("unused"))), OBJECT_MAPPER);

    @ParameterizedTest
    @CsvSource({"prd, prd.json", "architecture, architecture.json", "api, api.json"})
    void 예시_JSON은_스키마를_통과한다(String schema, String file) throws IOException {
        assertThat(validator.validate(schema, OBJECT_MAPPER.readTree(example(file)))).isEmpty();
    }

    @Test
    void 규칙을_어기면_어디가_틀렸는지_알려준다() throws IOException {
        ObjectNode api = (ObjectNode) OBJECT_MAPPER.readTree(example("api.json"));
        ((ObjectNode) api.withArray("resources").get(0)).put("path", "/categories");

        assertThat(validator.validate("api", api)).singleElement().asString().contains("path");
    }

    @Test
    void 예약된_필드명은_거부한다() throws IOException {
        ObjectNode architecture = (ObjectNode) OBJECT_MAPPER.readTree(example("architecture.json"));
        ((ObjectNode) architecture.withArray("entities").get(0).withArray("fields").get(0)).put("name", "id");

        assertThat(validator.validate("architecture", architecture)).isNotEmpty();
    }
}
