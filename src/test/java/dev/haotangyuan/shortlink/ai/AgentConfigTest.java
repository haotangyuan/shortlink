package dev.haotangyuan.shortlink.ai;

import dev.haotangyuan.shortlink.ai.tool.InsightTools;
import dev.haotangyuan.shortlink.ai.tool.StatsTools;
import io.agentscope.core.ReActAgent;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class AgentConfigTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withPropertyValues(
                    "short-link.ai.enabled=true",
                    "short-link.ai.api-key=test-key",
                    "short-link.ai.model-name=test-model",
                    "short-link.ai.base-url=http://localhost",
                    "short-link.ai.max-iters=2"
            )
            .withBean(StatsTools.class, () -> mock(StatsTools.class))
            .withBean(InsightTools.class, () -> mock(InsightTools.class))
            .withUserConfiguration(AgentConfig.class);

    @Test
    void createsAnIsolatedAgentForEachChatRequest() {
        contextRunner.run(context -> {
            ReActAgent first = context.getBean(ReActAgent.class);
            ReActAgent second = context.getBean(ReActAgent.class);

            assertThat(first).isNotSameAs(second);
        });
    }
}
