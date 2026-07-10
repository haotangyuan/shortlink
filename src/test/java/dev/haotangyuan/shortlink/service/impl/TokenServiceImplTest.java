package dev.haotangyuan.shortlink.service.impl;

import dev.haotangyuan.shortlink.common.biz.user.UserContext;
import dev.haotangyuan.shortlink.common.convention.exception.ClientException;
import dev.haotangyuan.shortlink.dao.entity.TokenDO;
import dev.haotangyuan.shortlink.dao.mapper.TokenMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import static dev.haotangyuan.shortlink.common.constant.RedisKeyConstant.API_TOKEN_HASH_KEY;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TokenServiceImplTest {

    @AfterEach
    void clearUserContext() {
        UserContext.removeUser();
    }

    @Test
    void deleteTokenDoesNotUpdateDatabaseWhenRedisRevocationFails() {
        TokenMapper tokenMapper = mock(TokenMapper.class);
        StringRedisTemplate stringRedisTemplate = mock(StringRedisTemplate.class);
        TokenServiceImpl service = new TokenServiceImpl(stringRedisTemplate);
        ReflectionTestUtils.setField(service, "baseMapper", tokenMapper);
        TokenDO token = TokenDO.builder()
                .id(1L)
                .username("alice")
                .tokenHash("hash")
                .build();
        token.setDelFlag(0);
        when(tokenMapper.selectById(1L)).thenReturn(token);
        when(stringRedisTemplate.delete(String.format(API_TOKEN_HASH_KEY, "hash")))
                .thenThrow(new IllegalStateException("redis unavailable"));
        UserContext.setUsername("alice");

        assertThrows(ClientException.class, () -> service.deleteToken(1L));

        verify(tokenMapper, never()).updateById(token);
    }

    @Test
    void disableTokenDoesNotUpdateDatabaseWhenRedisRevocationFails() {
        TokenMapper tokenMapper = mock(TokenMapper.class);
        StringRedisTemplate stringRedisTemplate = mock(StringRedisTemplate.class);
        TokenServiceImpl service = new TokenServiceImpl(stringRedisTemplate);
        ReflectionTestUtils.setField(service, "baseMapper", tokenMapper);
        TokenDO token = TokenDO.builder()
                .id(1L)
                .username("alice")
                .tokenHash("hash")
                .enableStatus(0)
                .build();
        token.setDelFlag(0);
        when(tokenMapper.selectById(1L)).thenReturn(token);
        when(stringRedisTemplate.delete(String.format(API_TOKEN_HASH_KEY, "hash")))
                .thenThrow(new IllegalStateException("redis unavailable"));
        UserContext.setUsername("alice");

        assertThrows(ClientException.class, () -> service.updateStatus(1L, false));

        verify(tokenMapper, never()).updateById(token);
    }
}
