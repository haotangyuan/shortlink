package dev.haotangyuan.shortlink.service.impl;

import dev.haotangyuan.shortlink.dao.mapper.GroupMapper;
import dev.haotangyuan.shortlink.dao.entity.UserDO;
import dev.haotangyuan.shortlink.dao.mapper.UserMapper;
import dev.haotangyuan.shortlink.dto.req.UserUpdateReqDTO;
import dev.haotangyuan.shortlink.common.biz.user.UserContext;
import dev.haotangyuan.shortlink.service.GroupService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import org.mockito.ArgumentCaptor;

import static dev.haotangyuan.shortlink.common.constant.RedisKeyConstant.SESSION_KEY;
import static dev.haotangyuan.shortlink.common.constant.RedisKeyConstant.USER_LOGIN_KEY;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.assertNull;

class UserServiceImplTest {

    @AfterEach
    void clearUserContext() {
        UserContext.removeUser();
    }

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

    @Test
    void updateDoesNotPersistAMaskedPhoneNumber() {
        UserMapper userMapper = mock(UserMapper.class);
        UserServiceImpl service = new UserServiceImpl(
                mock(RBloomFilter.class),
                mock(RedissonClient.class),
                mock(StringRedisTemplate.class),
                mock(GroupService.class),
                mock(GroupMapper.class)
        );
        ReflectionTestUtils.setField(service, "baseMapper", userMapper);
        UserUpdateReqDTO request = new UserUpdateReqDTO();
        request.setUsername("alice");
        request.setPhone("139****0001");
        UserContext.setUsername("alice");

        service.updateByUsername(request);

        ArgumentCaptor<UserDO> userCaptor = ArgumentCaptor.forClass(UserDO.class);
        verify(userMapper).update(userCaptor.capture(), org.mockito.ArgumentMatchers.any());
        assertNull(userCaptor.getValue().getPhone());
    }
}
