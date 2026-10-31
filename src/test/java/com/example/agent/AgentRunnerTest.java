package com.example.agent;

import static com.example.agent.AgentTestSupport.example;
import static com.example.agent.AgentTestSupport.properties;
import static com.example.agent.AgentTestSupport.runner;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.agent.ChatTurn.Role;
import com.example.spec.Prd;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class AgentRunnerTest {

    @Test
    void 시스템_프롬프트에_스킬_공통_규칙_스키마가_들어간다() throws IOException {
        FakeLlmClient llm = new FakeLlmClient(Map.of("prd", List.of(example("prd.json"))));

        planner(llm).plan("todo-service", "할 일 관리");

        String systemPrompt = llm.calls().get(0).systemPrompt();
        assertThat(systemPrompt)
                .startsWith("# PlannerAgent")
                .contains("## 에이전트 공통 규칙")
                .doesNotContain("## 네이밍")
                .contains("## 출력 JSON Schema")
                .contains("\"$id\": \"urn:project-templates:prd\"");
    }

    @Test
    void 코드_블록으로_감싼_JSON도_받아들인다() throws IOException {
        FakeLlmClient llm = new FakeLlmClient(Map.of("prd", List.of("```json\n" + example("prd.json") + "\n```")));

        Prd prd = planner(llm).plan("todo-service", "할 일 관리");

        assertThat(prd.title()).isEqualTo("할 일 관리 서비스");
        assertThat(llm.calls()).hasSize(1);
    }

    @Test
    void 검증에_실패하면_직전_출력과_오류를_붙여_다시_요청한다() throws IOException {
        String invalid = "{\"title\": \"할 일\"}";
        FakeLlmClient llm = new FakeLlmClient(Map.of("prd", List.of(invalid, example("prd.json"))));

        Prd prd = planner(llm).plan("todo-service", "할 일 관리");

        assertThat(prd.features()).hasSize(6);
        assertThat(llm.calls()).hasSize(2);
        List<ChatTurn> retry = llm.calls().get(1).conversation();
        assertThat(retry).extracting(ChatTurn::role).containsExactly(Role.USER, Role.ASSISTANT, Role.USER);
        assertThat(retry.get(0).content()).contains("\"projectName\" : \"todo-service\"");
        assertThat(retry.get(1).content()).isEqualTo(invalid);
        assertThat(retry.get(2).content()).contains("오류를 모두 고친 JSON").contains("summary");
    }

    @Test
    void 교차_검증_실패도_다시_요청한다() throws IOException {
        String duplicatedId = example("prd.json").replace("\"F-002\"", "\"F-001\"");
        FakeLlmClient llm = new FakeLlmClient(Map.of("prd", List.of(duplicatedId, example("prd.json"))));

        planner(llm).plan("todo-service", "할 일 관리");

        assertThat(llm.calls()).hasSize(2);
        assertThat(llm.calls().get(1).conversation().get(2).content()).contains("'F-001'가 중복");
    }

    @Test
    void 재요청_횟수를_다_쓰면_마지막_오류와_함께_실패한다() {
        FakeLlmClient llm = new FakeLlmClient(Map.of("prd", List.of("이건 JSON이 아닙니다")));

        assertThatThrownBy(() -> planner(llm).plan("todo-service", "할 일 관리"))
                .isInstanceOf(AgentFailedException.class)
                .hasMessageContaining("PlannerAgent 출력이 3번 모두")
                .hasMessageContaining("JSON이 아닙니다");
        assertThat(llm.calls()).hasSize(3);
    }

    private static PlannerAgent planner(FakeLlmClient llm) {
        return new PlannerAgent(runner(llm, properties(Path.of("unused"))), new CrossValidator());
    }
}
