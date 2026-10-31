package com.example;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.agent.SpringAiLlmClient;
import com.example.config.GeneratorProperties;
import com.example.pipeline.GenerationPipeline;
import com.example.template.TemplateRepository;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * 실제 설정({@code application.yml})으로 Bean이 연결되는지 확인한다.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class ApplicationContextTest {

    @Autowired
    GenerationPipeline pipeline;

    @Autowired
    SpringAiLlmClient llmClient;

    @Autowired
    GeneratorProperties properties;

    @Autowired
    TemplateRepository templateRepository;

    @Test
    void 파이프라인과_설정이_연결된다() {
        assertThat(pipeline).isNotNull();
        assertThat(llmClient).isNotNull();
        assertThat(properties.useTemplatesRepository()).isFalse();
        assertThat(templateRepository.templateDir()).isEqualTo(Path.of("project-templates/templates/spring-boot"));
        assertThat(properties.templateVersion()).isEqualTo("v1.0.0");
        assertThat(properties.maxRetries()).isEqualTo(2);
    }

    @Autowired
    com.example.input.slack.SlackProperties slackProperties;

    @Test
    void 토큰이_없으면_Slack은_꺼지고_허용_목록은_비어_있다() {
        assertThat(slackProperties.enabled()).isFalse();
        assertThat(slackProperties.command()).isEqualTo("/generate");
        assertThat(slackProperties.allowedUserIds()).isEmpty();
        assertThat(slackProperties.webUrl()).isEqualTo("http://localhost:8080");
    }
}
