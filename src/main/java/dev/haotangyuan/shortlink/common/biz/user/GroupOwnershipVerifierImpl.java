package dev.haotangyuan.shortlink.common.biz.user;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import dev.haotangyuan.shortlink.common.convention.exception.ClientException;
import dev.haotangyuan.shortlink.dao.entity.GroupDO;
import dev.haotangyuan.shortlink.dao.mapper.GroupMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static dev.haotangyuan.shortlink.common.constant.RedisKeyConstant.USER_GIDS_KEY;

@Service
@RequiredArgsConstructor
@Slf4j
public class GroupOwnershipVerifierImpl implements GroupOwnershipVerifier {

    private final StringRedisTemplate stringRedisTemplate;
    private final GroupMapper groupMapper;

    @Value("${short-link.session-ttl-minutes:30}")
    private int sessionTtlMinutes;

    @Override
    public void assertOwnedByCurrentUser(String gid) {
        if (StrUtil.isBlank(gid)) {
            throw new ClientException("分组标识为空");
        }
        String username = Optional.ofNullable(UserContext.getUsername())
                .orElseThrow(() -> new ClientException("用户未登录"));
        String setKey = String.format(USER_GIDS_KEY, username);
        Boolean hit = null;
        try {
            hit = stringRedisTemplate.opsForSet().isMember(setKey, gid);
        } catch (Exception ex) {
            log.error("Redis SISMEMBER user-gids error, username={}, gid={}", username, gid, ex);
        }
        if (Boolean.TRUE.equals(hit)) return;
        // 回源 DB 校验，并在缺失时回填索引（设置 TTL，但不做续期）
        LambdaQueryWrapper<GroupDO> queryWrapper = Wrappers.lambdaQuery(GroupDO.class)
                .eq(GroupDO::getGid, gid)
                .eq(GroupDO::getUsername, username)
                .eq(GroupDO::getDelFlag, 0);
        GroupDO group = groupMapper.selectOne(queryWrapper);
        if (group == null || StrUtil.isBlank(group.getUsername())) {
            throw new ClientException("用户信息与分组标识不匹配");
        }
        try {
            stringRedisTemplate.opsForSet().add(setKey, gid);
            stringRedisTemplate.expire(setKey, sessionTtlMinutes, TimeUnit.MINUTES);
        } catch (Exception ex) {
            log.error("Redis SADD/EXPIRE user-gids error, username={}, gid={}", username, gid, ex);
        }
    }

    @Override
    public void assertAllOwnedByCurrentUser(List<String> gids) {
        if (gids == null || gids.isEmpty()) return;
        gids.forEach(this::assertOwnedByCurrentUser);
    }
}
