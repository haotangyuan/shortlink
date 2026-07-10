package dev.haotangyuan.shortlink.service.impl;

import dev.haotangyuan.shortlink.common.biz.user.UserContext;
import dev.haotangyuan.shortlink.common.convention.exception.ClientException;
import dev.haotangyuan.shortlink.dao.entity.GroupDO;
import dev.haotangyuan.shortlink.dao.mapper.GroupMapper;
import dev.haotangyuan.shortlink.service.LinkService;
import dev.haotangyuan.shortlink.vo.GroupVO;
import dev.haotangyuan.shortlink.vo.GroupLinkCountQueryVO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GroupServiceImplTest {

    @AfterEach
    void clearUserContext() {
        UserContext.removeUser();
    }

    @Test
    void emptyGroupsHaveZeroLinks() {
        GroupMapper groupMapper = mock(GroupMapper.class);
        LinkService linkService = mock(LinkService.class);
        GroupServiceImpl service = new GroupServiceImpl(
                linkService,
                mock(RedissonClient.class),
                mock(StringRedisTemplate.class)
        );
        ReflectionTestUtils.setField(service, "baseMapper", groupMapper);
        when(groupMapper.selectList(any())).thenReturn(List.of(
                GroupDO.builder().gid("group-1").name("Empty").username("alice").build()
        ));
        when(linkService.listGroupLinkCount(List.of("group-1"))).thenReturn(Collections.emptyList());
        UserContext.setUsername("alice");

        List<GroupVO> groups = service.listGroup();

        assertEquals(0, groups.getFirst().getLinkCount());
    }

    @Test
    void groupWithLinksCannotBeDeleted() {
        GroupMapper groupMapper = mock(GroupMapper.class);
        LinkService linkService = mock(LinkService.class);
        GroupServiceImpl service = new GroupServiceImpl(
                linkService,
                mock(RedissonClient.class),
                mock(StringRedisTemplate.class)
        );
        ReflectionTestUtils.setField(service, "baseMapper", groupMapper);
        GroupLinkCountQueryVO count = new GroupLinkCountQueryVO();
        count.setGid("group-1");
        count.setLinkCount(1);
        when(linkService.listGroupLinkCount(List.of("group-1"))).thenReturn(List.of(count));
        UserContext.setUsername("alice");

        assertThrows(ClientException.class, () -> service.deleteGroup("group-1"));

        verify(groupMapper, never()).update(any(), any());
    }
}
