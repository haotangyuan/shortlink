package dev.haotangyuan.shortlink.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import dev.haotangyuan.shortlink.common.biz.user.UserContext;
import dev.haotangyuan.shortlink.common.convention.exception.ClientException;
import dev.haotangyuan.shortlink.dao.entity.GroupDO;
import dev.haotangyuan.shortlink.dao.mapper.GroupMapper;
import dev.haotangyuan.shortlink.dto.req.GroupSortReqDTO;
import dev.haotangyuan.shortlink.dto.req.GroupUpdateReqDTO;
import dev.haotangyuan.shortlink.vo.GroupLinkCountQueryVO;
import dev.haotangyuan.shortlink.vo.GroupVO;
import dev.haotangyuan.shortlink.service.GroupService;
import dev.haotangyuan.shortlink.service.LinkService;
import dev.haotangyuan.shortlink.toolkit.RandomGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import static dev.haotangyuan.shortlink.common.constant.RedisKeyConstant.LOCK_GROUP_CREATE_KEY;
import static dev.haotangyuan.shortlink.common.constant.RedisKeyConstant.USER_GIDS_KEY;

/**
 * 短链接分组接口实现层
 *
 * @author: haotangyuan
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GroupServiceImpl extends ServiceImpl<GroupMapper, GroupDO> implements GroupService {

    private final LinkService linkService;
    private final RedissonClient redissonClient;
    private final StringRedisTemplate stringRedisTemplate;

    @Value("${short-link.group.max-num}")
    private Integer groupMaxNum;

    @Value("${short-link.session-ttl-minutes:30}")
    private int sessionTtlMinutes;

    @Override
    public void saveGroup(String groupName) {
        saveGroup(UserContext.getUsername(), groupName);
    }

    @Override
    public void saveGroup(String username, String groupName) {
        String normalizedName = validateGroupName(groupName);
        RLock lock = redissonClient.getLock(String.format(LOCK_GROUP_CREATE_KEY, username));
        lock.lock();
        try {
            LambdaQueryWrapper<GroupDO> queryWrapper = Wrappers.lambdaQuery(GroupDO.class)
                    .eq(GroupDO::getUsername, username)
                    .eq(GroupDO::getDelFlag, 0);
            List<GroupDO> groupDOList = baseMapper.selectList(queryWrapper);
            if (CollUtil.isNotEmpty(groupDOList) && groupDOList.size() >= groupMaxNum) {
                throw new ClientException(String.format("已超出最大分组数：%d", groupMaxNum));
            }
            String gid;
            do {
                gid = RandomGenerator.generateRandom();
            } while (hasGid(username, gid));
            GroupDO groupDO = GroupDO.builder()
                    .gid(gid)
                    .sortOrder(0)
                    .username(username)
                    .name(normalizedName)
                    .build();
            if (baseMapper.insert(groupDO) < 1) {
                throw new ClientException("创建分组失败");
            }
            // 维护正向索引集合：user_gids
            try {
                String key = String.format(USER_GIDS_KEY, username);
                stringRedisTemplate.opsForSet().add(key, gid);
                stringRedisTemplate.expire(key, sessionTtlMinutes, java.util.concurrent.TimeUnit.MINUTES);
            } catch (Exception e) {
                log.error("Maintain user_gids on create error, username={}, gid={}", username, gid, e);
            }
        } finally {
            lock.unlock();
        }
    }

    private boolean hasGid(String username, String gid) {
        LambdaQueryWrapper<GroupDO> queryWrapper = Wrappers.lambdaQuery(GroupDO.class)
                .eq(GroupDO::getGid, gid)
                .eq(GroupDO::getUsername, username);
        GroupDO hasGroupFlag = baseMapper.selectOne(queryWrapper);
        return hasGroupFlag != null;
    }

    @Override
    public void updateGroup(GroupUpdateReqDTO groupUpdateReqDTO) {
        String normalizedName = validateGroupName(groupUpdateReqDTO.getName());
        LambdaUpdateWrapper<GroupDO> updateWrapper = Wrappers.lambdaUpdate(GroupDO.class)
                .eq(GroupDO::getDelFlag, 0)
                .eq(GroupDO::getUsername, UserContext.getUsername())
                .eq(GroupDO::getGid, groupUpdateReqDTO.getGid());
        GroupDO groupDO = new GroupDO();
        groupDO.setName(normalizedName);
        if (baseMapper.update(groupDO, updateWrapper) < 1) {
            throw new ClientException("分组不存在");
        }
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void deleteGroup(String gid) {
        List<GroupLinkCountQueryVO> groupLinkCounts = linkService.listGroupLinkCount(List.of(gid));
        boolean hasLinks = groupLinkCounts.stream()
                .anyMatch(each -> each.getLinkCount() != null && each.getLinkCount() > 0);
        if (hasLinks) {
            throw new ClientException("请先移动或删除分组内的短链接");
        }
        LambdaUpdateWrapper<GroupDO> updateWrapper = Wrappers.lambdaUpdate(GroupDO.class)
                .eq(GroupDO::getDelFlag, 0)
                .eq(GroupDO::getUsername, UserContext.getUsername())
                .eq(GroupDO::getGid, gid);
        GroupDO groupDO = new GroupDO();
        groupDO.setDelFlag(1);
        if (baseMapper.update(groupDO, updateWrapper) < 1) {
            throw new ClientException("分组不存在");
        }
        try {
            String key = String.format(USER_GIDS_KEY, UserContext.getUsername());
            stringRedisTemplate.opsForSet().remove(key, gid);
        } catch (Exception e) {
            log.error("Maintain user_gids on delete error, username={}, gid={}", UserContext.getUsername(), gid, e);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void sortGroup(List<GroupSortReqDTO> groupSortReqDTOs) {
        if (groupSortReqDTOs == null || groupSortReqDTOs.size() > groupMaxNum) {
            throw new ClientException("分组排序参数不正确");
        }
        groupSortReqDTOs.forEach(groupSortReqDTO -> {
            GroupDO groupDO = GroupDO.builder()
                    .sortOrder(groupSortReqDTO.getSortOrder())
                    .build();
            LambdaUpdateWrapper<GroupDO> updateWrapper = Wrappers.lambdaUpdate(GroupDO.class)
                    .eq(GroupDO::getDelFlag, 0)
                    .eq(GroupDO::getUsername, UserContext.getUsername())
                    .eq(GroupDO::getGid, groupSortReqDTO.getGid());
            if (baseMapper.update(groupDO, updateWrapper) < 1) {
                throw new ClientException("分组不存在");
            }
        });
    }

    @Override
    public List<GroupVO> listGroup() {
        Wrapper<GroupDO> queryWrapper = Wrappers.lambdaQuery(GroupDO.class)
                .eq(GroupDO::getDelFlag, 0)
                .eq(GroupDO::getUsername, UserContext.getUsername())
                .orderByDesc(GroupDO::getSortOrder, GroupDO::getUpdateTime);
        List<GroupDO> groupDOList = baseMapper.selectList(queryWrapper);
        List<GroupLinkCountQueryVO> listResult = linkService
                .listGroupLinkCount(groupDOList.stream().map(GroupDO::getGid).toList());
        List<GroupVO> groupRespDTOList = BeanUtil.copyToList(groupDOList, GroupVO.class);
        Map<String, GroupLinkCountQueryVO> countMap = listResult.stream()
                .collect(Collectors.toMap(GroupLinkCountQueryVO::getGid, Function.identity()));
        groupRespDTOList.forEach(each -> {
            GroupLinkCountQueryVO count = countMap.get(each.getGid());
            each.setLinkCount(count == null ? 0 : count.getLinkCount());
            each.setTotalPv(count == null || count.getTotalPv() == null ? 0L : count.getTotalPv());
            each.setTodayPv(count == null || count.getTodayPv() == null ? 0L : count.getTodayPv());
        });
        return groupRespDTOList;
    }

    private String validateGroupName(String groupName) {
        String normalizedName = StrUtil.trim(groupName);
        if (StrUtil.isBlank(normalizedName) || normalizedName.length() > 64) {
            throw new ClientException("分组名称长度应为 1-64 个字符");
        }
        return normalizedName;
    }
}
