package dev.haotangyuan.shortlink.ai.tool;

import dev.haotangyuan.shortlink.common.biz.user.UserContext;
import dev.haotangyuan.shortlink.service.GroupService;
import dev.haotangyuan.shortlink.service.LinkService;
import dev.haotangyuan.shortlink.service.LinkStatsService;
import dev.haotangyuan.shortlink.vo.GroupVO;
import io.agentscope.core.agent.RuntimeContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class StatsToolsTest {

    @AfterEach
    void clearUserContext() {
        UserContext.removeUser();
    }

    @Test
    void toolCallsUseTheRuntimeContextUserWithoutLeakingThreadState() {
        GroupService groupService = mock(GroupService.class);
        when(groupService.listGroup()).thenAnswer(invocation -> {
            assertTrue("alice".equals(UserContext.getUsername()));
            GroupVO group = new GroupVO();
            group.setGid("group-1");
            group.setName("默认分组");
            return List.of(group);
        });
        StatsTools tools = new StatsTools(
                mock(LinkStatsService.class),
                mock(LinkService.class),
                groupService
        );
        RuntimeContext context = RuntimeContext.builder()
                .userId("alice")
                .sessionId("session-1")
                .build();

        String result = tools.listGroups(context);

        assertTrue(result.contains("group-1"));
        assertNull(UserContext.getUsername());
    }
}
