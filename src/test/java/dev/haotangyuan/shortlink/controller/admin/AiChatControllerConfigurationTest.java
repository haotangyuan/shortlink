package dev.haotangyuan.shortlink.controller.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.haotangyuan.shortlink.service.AiSessionService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class AiChatControllerConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withPropertyValues("short-link.ai.enabled=true")
            .withBean(AiSessionService.class, () -> mock(AiSessionService.class))
            .withBean(ObjectMapper.class, ObjectMapper::new)
            .withUserConfiguration(AiChatController.class);

    @Test
    void startsWithoutAgentWhenApiKeyIsMissing() {
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(AiChatController.class);
        });
    }
}
