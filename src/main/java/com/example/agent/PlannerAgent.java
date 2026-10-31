package com.example.agent;

import com.example.spec.Prd;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 요구사항 원문 → PRD.
 */
@Component
@RequiredArgsConstructor
public class PlannerAgent {

    private final AgentRunner runner;
    private final CrossValidator crossValidator;

    public Prd plan(String projectName, String requirements) {
        Map<String, Object> input = new LinkedHashMap<>();
        input.put("projectName", projectName);
        input.put("requirements", requirements);
        return runner.run("PlannerAgent", "planner", "prd", input, Prd.class, crossValidator::checkPrd);
    }
}
