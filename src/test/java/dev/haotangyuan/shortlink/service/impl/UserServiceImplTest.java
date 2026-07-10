package dev.haotangyuan.shortlink.service.impl;

import dev.haotangyuan.shortlink.dao.mapper.GroupMapper;
import dev.haotangyuan.shortlink.dao.entity.UserDO;
import dev.haotangyuan.shortlink.dao.mapper.UserMapper;
import dev.haotangyuan.shortlink.dto.req.UserUpdateReqDTO;
import dev.haotangyuan.shortlink.common.biz.user.UserContext;
import dev.haotangyuan.shortlink.common.convention.exception.ClientException;
import dev.haotangyuan.shortlink.service.GroupService;
import dev.haotangyuan.shortlink.vo.UserVO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import org.mockito.ArgumentCaptor;

import static dev.haotangyuan.shortlink.common.constant.RedisKeyConstant.SESSION_KEY;
import static dev.haotangyuan.shortlink.common.constant.RedisKeyConstant.USER_LOGIN_KEY;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        when(stringRedisTemplate.opsForHash()).thenReturn(hashOperations);
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(String.format(SESSION_KEY, "session-token"))).thenReturn("alice");
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
    void logoutRejectsAMismatchedUsername() {
        StringRedisTemplate stringRedisTemplate = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(String.format(SESSION_KEY, "session-token"))).thenReturn("alice");
        UserServiceImpl service = new UserServiceImpl(
                mock(RBloomFilter.class),
                mock(RedissonClient.class),
                stringRedisTemplate,
                mock(GroupService.class),
                mock(GroupMapper.class)
        );

        assertThrows(ClientException.class, () -> service.logout("bob", "session-token"));

        verify(stringRedisTemplate, never()).delete(String.format(SESSION_KEY, "session-token"));
    }

    @Test
    void bloomFilterPositiveIsVerifiedAgainstTheDatabase() {
        RBloomFilter<String> bloomFilter = mock(RBloomFilter.class);
        UserMapper userMapper = mock(UserMapper.class);
        when(bloomFilter.contains("alice")).thenReturn(true);
        when(userMapper.selectCount(any())).thenReturn(0L);
        UserServiceImpl service = new UserServiceImpl(
                bloomFilter,
                mock(RedissonClient.class),
                mock(StringRedisTemplate.class),
                mock(GroupService.class),
                mock(GroupMapper.class)
        );
        ReflectionTestUtils.setField(service, "baseMapper", userMapper);

        assertFalse(service.existsByUsername("alice"));

        when(userMapper.selectCount(any())).thenReturn(1L);
        assertTrue(service.existsByUsername("alice"));
    }

    @Test
    void userProfileIsRestrictedToTheCurrentUser() {
        UserMapper userMapper = mock(UserMapper.class);
        UserServiceImpl service = new UserServiceImpl(
                mock(RBloomFilter.class),
                mock(RedissonClient.class),
                mock(StringRedisTemplate.class),
                mock(GroupService.class),
                mock(GroupMapper.class)
        );
        ReflectionTestUtils.setField(service, "baseMapper", userMapper);
        UserContext.setUsername("alice");

        assertThrows(ClientException.class, () -> service.getByUsername("bob"));

        verify(userMapper, never()).selectOne(any());
    }

    @Test
    void currentUserCanReadTheirProfile() {
        UserMapper userMapper = mock(UserMapper.class);
        UserDO userDO = new UserDO();
        userDO.setUsername("alice");
        when(userMapper.selectOne(any())).thenReturn(userDO);
        UserServiceImpl service = new UserServiceImpl(
                mock(RBloomFilter.class),
                mock(RedissonClient.class),
                mock(StringRedisTemplate.class),
                mock(GroupService.class),
                mock(GroupMapper.class)
        );
        ReflectionTestUtils.setField(service, "baseMapper", userMapper);
        UserContext.setUsername("alice");

        UserVO user = service.getByUsername("alice");

        assertTrue("alice".equals(user.getUsername()));
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
        when(userMapper.update(any(), any())).thenReturn(1);
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
