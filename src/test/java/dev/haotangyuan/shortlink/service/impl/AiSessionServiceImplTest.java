package dev.haotangyuan.shortlink.service.impl;

import dev.haotangyuan.shortlink.common.biz.user.UserContext;
import dev.haotangyuan.shortlink.common.convention.exception.ClientException;
import dev.haotangyuan.shortlink.dao.entity.AiSessionDO;
import dev.haotangyuan.shortlink.dao.mapper.AiMessageMapper;
import dev.haotangyuan.shortlink.dao.mapper.AiSessionMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiSessionServiceImplTest {

    @Mock
    private AiSessionMapper aiSessionMapper;
    @Mock
    private AiMessageMapper aiMessageMapper;

    private AiSessionServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new AiSessionServiceImpl(aiMessageMapper);
        ReflectionTestUtils.setField(service, "baseMapper", aiSessionMapper);
        UserContext.setUsername("alice");
    }

    @AfterEach
    void tearDown() {
        UserContext.removeUser();
    }

    @Test
    void rejectsExistingSessionOwnedByAnotherUser() {
        AiSessionDO existingSession = AiSessionDO.builder()
                .sessionId("shared-session")
                .username("bob")
                .build();
        when(aiSessionMapper.selectOne(any())).thenReturn(existingSession);

        assertThrows(ClientException.class, () -> service.getOrCreateSession("shared-session"));

        verify(aiSessionMapper, never()).insert(any(AiSessionDO.class));
    }
}
