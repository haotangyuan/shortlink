package dev.haotangyuan.shortlink.service.impl;

import com.github.benmanes.caffeine.cache.Cache;
import dev.haotangyuan.shortlink.common.biz.user.GroupOwnershipVerifier;
import dev.haotangyuan.shortlink.common.config.GotoDomainWhiteListConfiguration;
import dev.haotangyuan.shortlink.dao.mapper.LinkAccessStatsMapper;
import dev.haotangyuan.shortlink.dao.mapper.LinkGotoMapper;
import dev.haotangyuan.shortlink.dao.mapper.LinkMapper;
import dev.haotangyuan.shortlink.dto.req.LinkUpdateReqDTO;
import dev.haotangyuan.shortlink.mq.consumer.LinkStatsSaver;
import dev.haotangyuan.shortlink.mq.producer.LinkStatsSaveProducer;
import dev.haotangyuan.shortlink.toolkit.LinkUtil;
import org.junit.jupiter.api.Test;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.concurrent.locks.ReentrantLock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LinkServiceImplTest {

    @Test
    @SuppressWarnings("unchecked")
    void updateAcceptsFullShortUrlReturnedByCreate() {
        LinkMapper linkMapper = mock(LinkMapper.class);
        GotoDomainWhiteListConfiguration whiteList = mock(GotoDomainWhiteListConfiguration.class);
        when(whiteList.getEnable()).thenReturn(false);
        LinkServiceImpl service = new LinkServiceImpl(
                mock(RBloomFilter.class),
                mock(LinkGotoMapper.class),
                mock(StringRedisTemplate.class),
                mock(RedissonClient.class),
                mock(LinkAccessStatsMapper.class),
                whiteList,
                mock(LinkStatsSaveProducer.class),
                mock(LinkStatsSaver.class),
                mock(GroupOwnershipVerifier.class),
                mock(LinkUtil.class),
                mock(Cache.class),
                mock(Cache.class)
        );
        ReflectionTestUtils.setField(service, "baseMapper", linkMapper);
        when(linkMapper.selectOne(any())).thenThrow(new IllegalStateException("stop after lookup"));
        LinkUpdateReqDTO request = new LinkUpdateReqDTO();
        request.setOriginGid("group-1");
        request.setGid("group-1");
        request.setFullShortUrl("http://127.0.0.1:8068/abc123");
        request.setOriginUrl("https://github.com/openai");
        request.setValidDateType(0);

        assertThrows(IllegalStateException.class, () -> service.updateLink(request));

        assertEquals("127.0.0.1:8068/abc123", request.getFullShortUrl());
    }
}
