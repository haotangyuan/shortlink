package dev.haotangyuan.shortlink.service.impl;

import dev.haotangyuan.shortlink.dao.mapper.GroupMapper;
import dev.haotangyuan.shortlink.service.GroupService;
import org.junit.jupiter.api.Test;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

import static dev.haotangyuan.shortlink.common.constant.RedisKeyConstant.SESSION_KEY;
import static dev.haotangyuan.shortlink.common.constant.RedisKeyConstant.USER_LOGIN_KEY;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserServiceImplTest {

    @Test
    @SuppressWarnings("unchecked")
    void logoutRemovesSessionAndReusableTokenIndex() {
        RBloomFilter<String> bloomFilter = mock(RBloomFilter.class);
        RedissonClient redissonClient = mock(RedissonClient.class);
        StringRedisTemplate stringRedisTemplate = mock(StringRedisTemplate.class);
        GroupService groupService = mock(GroupService.class);
        GroupMapper groupMapper = mock(GroupMapper.class);
        @SuppressWarnings("unchecked")
        HashOperations<String, Object, Object> hashOperations = mock(HashOperations.class);
        when(stringRedisTemplate.opsForHash()).thenReturn(hashOperations);
        UserServiceImpl service = new UserServiceImpl(
                bloomFilter,
                redissonClient,
                stringRedisTemplate,
                groupService,
                groupMapper
        );

        service.logout("alice", "session-token");

        verify(stringRedisTemplate).delete(String.format(SESSION_KEY, "session-token"));
        verify(hashOperations).delete(USER_LOGIN_KEY + "alice", "session-token");
    }
}
