package dev.haotangyuan.shortlink.service.impl;

import dev.haotangyuan.shortlink.common.biz.user.GroupOwnershipVerifier;
import dev.haotangyuan.shortlink.dao.entity.LinkDO;
import dev.haotangyuan.shortlink.dao.mapper.LinkAccessStatsMapper;
import dev.haotangyuan.shortlink.dao.mapper.LinkMapper;
import dev.haotangyuan.shortlink.dto.req.RecycleBinSaveReqDTO;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class RecycleBinServiceImplTest {

    @Test
    void recordsTheTimeWhenALinkEntersTheRecycleBin() {
        LinkMapper linkMapper = mock(LinkMapper.class);
        GroupOwnershipVerifier ownershipVerifier = mock(GroupOwnershipVerifier.class);
        RecycleBinServiceImpl service = new RecycleBinServiceImpl(
                mock(StringRedisTemplate.class),
                ownershipVerifier,
                mock(LinkAccessStatsMapper.class)
        );
        ReflectionTestUtils.setField(service, "baseMapper", linkMapper);
        RecycleBinSaveReqDTO request = new RecycleBinSaveReqDTO();
        request.setGid("group-1");
        request.setFullShortUrl("short.example/abc123");
        long before = System.currentTimeMillis();

        service.saveRecycledBin(request);

        ArgumentCaptor<LinkDO> linkCaptor = ArgumentCaptor.forClass(LinkDO.class);
        verify(linkMapper).update(linkCaptor.capture(), any());
        LinkDO updatedLink = linkCaptor.getValue();
        assertEquals(1, updatedLink.getEnableStatus());
        assertTrue(updatedLink.getDelTime() >= before);
        verify(ownershipVerifier).assertOwnedByCurrentUser("group-1");
    }
}
