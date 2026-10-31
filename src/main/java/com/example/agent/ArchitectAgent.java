package com.example.agent;

import com.example.spec.Architecture;
import com.example.spec.Prd;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * PRD → 도메인 엔티티와 관계.
 */
@Component
@RequiredArgsConstructor
public class ArchitectAgent {

    private final AgentRunner runner;
    private final CrossValidator crossValidator;

    public Architecture design(Prd prd) {
        return runner.run("ArchitectAgent", "architect", "architecture", prd, Architecture.class,
                architecture -> crossValidator.checkArchitecture(prd, architecture));
    }
}
