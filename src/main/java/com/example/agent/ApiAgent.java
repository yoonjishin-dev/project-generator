package com.example.agent;

import com.example.spec.ApiSpec;
import com.example.spec.Architecture;
import com.example.spec.Prd;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * PRD + 엔티티 → 엔티티별 REST 리소스와 작업.
 */
@Component
@RequiredArgsConstructor
public class ApiAgent {

    private final AgentRunner runner;
    private final CrossValidator crossValidator;

    public ApiSpec design(Prd prd, Architecture architecture) {
        Map<String, Object> input = new LinkedHashMap<>();
        input.put("prd", prd);
        input.put("architecture", architecture);
        return runner.run("ApiAgent", "api", "api", input, ApiSpec.class,
                api -> crossValidator.checkApi(architecture, api));
    }
}
