package dev.haotangyuan.shortlink.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
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
import dev.haotangyuan.shortlink.dto.resp.GroupLinkCountQueryRespDTO;
import dev.haotangyuan.shortlink.dto.resp.GroupRespDTO;
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

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static dev.haotangyuan.shortlink.common.constant.RedisKeyConstant.LOCK_GROUP_CREATE_KEY;
import static dev.haotangyuan.shortlink.common.constant.RedisKeyConstant.USER_GIDS_KEY;

/**
 * 短链接分组接口实现层
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

    @Override
    public void saveGroup(String groupName) {
        saveGroup(UserContext.getUsername(), groupName);
    }

    @Override
    public void saveGroup(String username, String groupName) {
        RLock lock = redissonClient.getLock(String.format(LOCK_GROUP_CREATE_KEY, username));
        lock.lock();
        try {
            LambdaQueryWrapper<GroupDO> queryWrapper = Wrappers.lambdaQuery(GroupDO.class)
                    .eq(GroupDO::getUsername, username)
                    .eq(GroupDO::getDelFlag, 0);
            List<GroupDO> groupDOList = baseMapper.selectList(queryWrapper);
            if (CollUtil.isNotEmpty(groupDOList) && groupDOList.size() == groupMaxNum) {
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
                    .name(groupName)
                    .build();
            baseMapper.insert(groupDO);
            // 维护正向索引集合：user_gids
            try {
                String key = String.format(USER_GIDS_KEY, username);
                stringRedisTemplate.opsForSet().add(key, gid);
                stringRedisTemplate.expire(key, 30, java.util.concurrent.TimeUnit.MINUTES);
            } catch (Throwable t) {
                log.error("Maintain user_gids on create error, username={}, gid={}", username, gid, t);
            }
        } finally {
            lock.unlock();
        }
    }

    private boolean hasGid(String gid) {
        return hasGid(UserContext.getUsername(), gid);
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
        LambdaUpdateWrapper<GroupDO> updateWrapper = Wrappers.lambdaUpdate(GroupDO.class)
                .eq(GroupDO::getDelFlag, 0)
                .eq(GroupDO::getUsername, UserContext.getUsername())
                .eq(GroupDO::getGid, groupUpdateReqDTO.getGid());
        GroupDO groupDO = new GroupDO();
        groupDO.setName(groupUpdateReqDTO.getName());
        baseMapper.update(groupDO, updateWrapper);
    }

    @Override
    public void deleteGroup(String gid) {
        LambdaUpdateWrapper<GroupDO> updateWrapper = Wrappers.lambdaUpdate(GroupDO.class)
                .eq(GroupDO::getDelFlag, 0)
                .eq(GroupDO::getUsername, UserContext.getUsername())
                .eq(GroupDO::getGid, gid);
        GroupDO groupDO = new GroupDO();
        groupDO.setDelFlag(1);
        baseMapper.update(groupDO, updateWrapper);
        try {
            String key = String.format(USER_GIDS_KEY, UserContext.getUsername());
            stringRedisTemplate.opsForSet().remove(key, gid);
        } catch (Throwable t) {
            log.error("Maintain user_gids on delete error, username={}, gid={}", UserContext.getUsername(), gid, t);
        }
    }

    @Override
    public void sortGroup(List<GroupSortReqDTO> groupSortReqDTOs) {
        groupSortReqDTOs.forEach(groupSortReqDTO -> {
            GroupDO groupDO = GroupDO.builder()
                    .sortOrder(groupSortReqDTO.getSortOrder())
                    .build();
            LambdaUpdateWrapper<GroupDO> updateWrapper = Wrappers.lambdaUpdate(GroupDO.class)
                    .eq(GroupDO::getDelFlag, 0)
                    .eq(GroupDO::getUsername, UserContext.getUsername())
                    .eq(GroupDO::getGid, groupSortReqDTO.getGid());
            baseMapper.update(groupDO, updateWrapper);
        });
    }

    @Override
    public List<GroupRespDTO> listGroup() {
        Wrapper<GroupDO> queryWrapper = Wrappers.lambdaQuery(GroupDO.class)
                .eq(GroupDO::getDelFlag, 0)
                .eq(GroupDO::getUsername, UserContext.getUsername())
                .orderByDesc(GroupDO::getSortOrder, GroupDO::getUpdateTime);
        List<GroupDO> groupDOList = baseMapper.selectList(queryWrapper);
        List<GroupLinkCountQueryRespDTO> listResult = linkService
                .listGroupLinkCount(groupDOList.stream().map(GroupDO::getGid).toList());
        List<GroupRespDTO> groupRespDTOList = BeanUtil.copyToList(groupDOList, GroupRespDTO.class);
        groupRespDTOList.forEach(each -> {
            Optional<GroupLinkCountQueryRespDTO> first = listResult.stream()
                    .filter(item -> Objects.equals(item.getGid(), each.getGid()))
                    .findFirst();
            first.ifPresent(item -> {each.setLinkCount(first.get().getLinkCount());});
        });
        return groupRespDTOList;
    }
}
