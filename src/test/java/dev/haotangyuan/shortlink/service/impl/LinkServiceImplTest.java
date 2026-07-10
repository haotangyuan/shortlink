package dev.haotangyuan.shortlink.service.impl;

import com.github.benmanes.caffeine.cache.Cache;
import dev.haotangyuan.shortlink.common.biz.user.GroupOwnershipVerifier;
import dev.haotangyuan.shortlink.common.config.GotoDomainWhiteListConfiguration;
import dev.haotangyuan.shortlink.common.convention.exception.ClientException;
import dev.haotangyuan.shortlink.dao.entity.LinkDO;
import dev.haotangyuan.shortlink.dao.mapper.LinkAccessStatsMapper;
import dev.haotangyuan.shortlink.dao.mapper.LinkGotoMapper;
import dev.haotangyuan.shortlink.dao.mapper.LinkMapper;
import dev.haotangyuan.shortlink.dto.req.LinkBatchCreateReqDTO;
import dev.haotangyuan.shortlink.dto.req.LinkUpdateReqDTO;
import dev.haotangyuan.shortlink.mq.consumer.LinkStatsSaver;
import dev.haotangyuan.shortlink.mq.producer.LinkStatsSaveProducer;
import dev.haotangyuan.shortlink.toolkit.LinkUtil;
import dev.haotangyuan.shortlink.vo.LinkBatchCreateVO;
import dev.haotangyuan.shortlink.vo.LinkCreateVO;
import org.junit.jupiter.api.Test;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RLock;
import org.redisson.api.RReadWriteLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionTemplate;

import org.mockito.ArgumentCaptor;

import java.util.Collections;
import java.util.Date;
import java.util.concurrent.locks.ReentrantLock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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
                mock(Cache.class),
                mock(TransactionTemplate.class)
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

    @Test
    @SuppressWarnings("unchecked")
    void movingLinkUpdatesTheRouteInPlace() {
        LinkMapper linkMapper = mock(LinkMapper.class);
        LinkGotoMapper linkGotoMapper = mock(LinkGotoMapper.class);
        RedissonClient redissonClient = mock(RedissonClient.class);
        RReadWriteLock readWriteLock = mock(RReadWriteLock.class);
        RLock writeLock = mock(RLock.class);
        when(redissonClient.getReadWriteLock(anyString())).thenReturn(readWriteLock);
        when(readWriteLock.writeLock()).thenReturn(writeLock);
        GotoDomainWhiteListConfiguration whiteList = mock(GotoDomainWhiteListConfiguration.class);
        when(whiteList.getEnable()).thenReturn(false);
        LinkStatsSaver linkStatsSaver = mock(LinkStatsSaver.class);
        LinkServiceImpl service = new LinkServiceImpl(
                mock(RBloomFilter.class),
                linkGotoMapper,
                mock(StringRedisTemplate.class),
                redissonClient,
                mock(LinkAccessStatsMapper.class),
                whiteList,
                mock(LinkStatsSaveProducer.class),
                linkStatsSaver,
                mock(GroupOwnershipVerifier.class),
                mock(LinkUtil.class),
                mock(Cache.class),
                mock(Cache.class),
                mock(TransactionTemplate.class)
        );
        ReflectionTestUtils.setField(service, "baseMapper", linkMapper);
        Date validDate = new Date(System.currentTimeMillis() + 86_400_000L);
        LinkDO existing = LinkDO.builder()
                .domain("short.example")
                .shortUri("abc123")
                .fullShortUrl("short.example/abc123")
                .originUrl("https://github.com/openai")
                .gid("group-1")
                .createdType(0)
                .validDateType(1)
                .validDate(validDate)
                .enableStatus(0)
                .totalPv(1)
                .totalUv(1)
                .totalUip(1)
                .build();
        when(linkMapper.selectOne(any())).thenReturn(existing);
        when(linkMapper.update(any(), any())).thenReturn(1);
        when(linkMapper.insert(any())).thenReturn(1);
        when(linkGotoMapper.update(any(), any())).thenReturn(1);
        LinkUpdateReqDTO request = new LinkUpdateReqDTO();
        request.setOriginGid("group-1");
        request.setGid("group-2");
        request.setFullShortUrl("short.example/abc123");
        request.setOriginUrl(existing.getOriginUrl());
        request.setValidDateType(1);
        request.setValidDate(validDate);

        service.updateLink(request);

        verify(linkGotoMapper).update(any(), any());
        verify(linkGotoMapper, never()).selectOne(any());
        verify(linkGotoMapper, never()).delete(any());
        verify(linkGotoMapper, never()).insert(any());
        verify(linkStatsSaver).invalidateGidCache("short.example/abc123");
        ArgumentCaptor<LinkDO> insertedLink = ArgumentCaptor.forClass(LinkDO.class);
        verify(linkMapper).insert(insertedLink.capture());
        assertEquals("short.example", insertedLink.getValue().getDomain());
    }

    @Test
    @SuppressWarnings("unchecked")
    void batchCreateRejectsOversizedRequests() {
        LinkServiceImpl service = new LinkServiceImpl(
                mock(RBloomFilter.class),
                mock(LinkGotoMapper.class),
                mock(StringRedisTemplate.class),
                mock(RedissonClient.class),
                mock(LinkAccessStatsMapper.class),
                mock(GotoDomainWhiteListConfiguration.class),
                mock(LinkStatsSaveProducer.class),
                mock(LinkStatsSaver.class),
                mock(GroupOwnershipVerifier.class),
                mock(LinkUtil.class),
                mock(Cache.class),
                mock(Cache.class),
                mock(TransactionTemplate.class)
        );
        LinkBatchCreateReqDTO request = new LinkBatchCreateReqDTO();
        request.setOriginUrls(Collections.nCopies(101, "https://example.com"));

        assertThrows(ClientException.class, () -> service.batchCreateLink(request));
    }

    @Test
    @SuppressWarnings("unchecked")
    void batchCreateRunsEachItemInTransaction() {
        TransactionTemplate transactionTemplate = mock(TransactionTemplate.class);
        when(transactionTemplate.execute(any())).thenReturn(LinkCreateVO.builder()
                .fullShortUrl("http://127.0.0.1:8068/abc123")
                .originUrl("https://example.com")
                .gid("group-1")
                .build());
        LinkServiceImpl service = new LinkServiceImpl(
                mock(RBloomFilter.class),
                mock(LinkGotoMapper.class),
                mock(StringRedisTemplate.class),
                mock(RedissonClient.class),
                mock(LinkAccessStatsMapper.class),
                mock(GotoDomainWhiteListConfiguration.class),
                mock(LinkStatsSaveProducer.class),
                mock(LinkStatsSaver.class),
                mock(GroupOwnershipVerifier.class),
                mock(LinkUtil.class),
                mock(Cache.class),
                mock(Cache.class),
                transactionTemplate
        );
        LinkBatchCreateReqDTO request = new LinkBatchCreateReqDTO();
        request.setGid("group-1");
        request.setOriginUrls(Collections.singletonList("https://example.com"));

        LinkBatchCreateVO result = service.batchCreateLink(request);

        assertEquals(1, result.getTotal());
        verify(transactionTemplate).execute(any());
    }
}
