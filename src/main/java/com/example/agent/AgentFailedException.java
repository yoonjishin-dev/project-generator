package com.example.agent;

import java.util.List;

/**
 * 재요청 횟수를 다 써도 에이전트 출력이 검증을 통과하지 못했다.
 */
public class AgentFailedException extends RuntimeException {

    private final String agent;
    private final List<String> errors;

    public AgentFailedException(String agent, int attempts, List<String> errors) {
        super(agent + " 출력이 " + attempts + "번 모두 검증에 실패했습니다:\n- " + String.join("\n- ", errors));
        this.agent = agent;
        this.errors = List.copyOf(errors);
    }

    public String agent() {
        return agent;
    }

    public List<String> errors() {
        return errors;
    }
}
