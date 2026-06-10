package com.intelligentdoctor.chat.agent;

import com.intelligentdoctor.config.AppProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class TriageAgentRuntimeSelector {

    private final AppProperties properties;
    private final List<TriageAgentRuntime> runtimes;

    public TriageAgentRuntimeSelector(AppProperties properties, List<TriageAgentRuntime> runtimes) {
        this.properties = properties;
        this.runtimes = runtimes;
    }

    public TriageAgentRuntime select() {
        String configured = properties.getAgent().getRuntime();
        return runtimes.stream()
                .filter(runtime -> runtime.supports(configured))
                .findFirst()
                .orElseGet(() -> runtimes.stream()
                        .filter(runtime -> runtime.supports("native"))
                        .findFirst()
                        .orElseThrow(() -> new IllegalStateException("No triage agent runtime is available")));
    }
}
