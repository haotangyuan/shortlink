package dev.haotangyuan.shortlink.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.lang.UUID;
import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import dev.haotangyuan.shortlink.common.convention.errorcode.BaseErrorCode;
import dev.haotangyuan.shortlink.common.convention.exception.ClientException;
import dev.haotangyuan.shortlink.common.biz.user.UserContext;
import dev.haotangyuan.shortlink.toolkit.HashUtil;
import dev.haotangyuan.shortlink.dao.entity.GroupDO;
import dev.haotangyuan.shortlink.dao.entity.UserDO;
import dev.haotangyuan.shortlink.dao.mapper.GroupMapper;
import dev.haotangyuan.shortlink.dao.mapper.UserMapper;
import dev.haotangyuan.shortlink.dto.req.UserLoginReqDTO;
import dev.haotangyuan.shortlink.dto.req.UserRegisterReqDTO;
import dev.haotangyuan.shortlink.dto.req.UserUpdateReqDTO;
import dev.haotangyuan.shortlink.vo.UserLoginVO;
import dev.haotangyuan.shortlink.vo.UserVO;
import dev.haotangyuan.shortlink.service.GroupService;
import dev.haotangyuan.shortlink.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static dev.haotangyuan.shortlink.common.constant.RedisKeyConstant.*;
import static dev.haotangyuan.shortlink.common.convention.errorcode.BaseErrorCode.*;

/**
 * 用户接口实现层
 *
 * @author: haotangyuan
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, UserDO> implements UserService {

    private final RBloomFilter<String> userRegisterCachePenetrationBloomFilter;
    private final RedissonClient redissonClient;
    private final StringRedisTemplate stringRedisTemplate;
    private final GroupService groupService;
    private final GroupMapper groupMapper;

    @Value("${short-link.session-ttl-minutes:30}")
    private int sessionTtlMinutes;

    private static final String USER_GIDS_REFRESH_LUA_SCRIPT_PATH = "lua/user_gids_refresh.lua";
    private static final DefaultRedisScript<Long> USER_GIDS_REFRESH_SCRIPT;

    static {
        USER_GIDS_REFRESH_SCRIPT = new DefaultRedisScript<>();
        USER_GIDS_REFRESH_SCRIPT.setResultType(Long.class);
        USER_GIDS_REFRESH_SCRIPT.setScriptSource(new ResourceScriptSource(new ClassPathResource(USER_GIDS_REFRESH_LUA_SCRIPT_PATH)));
    }

    @Override
    public UserVO getByUsername(String username) {
        LambdaQueryWrapper<UserDO> queryWrapper = Wrappers.lambdaQuery(UserDO.class)
                .eq(UserDO::getUsername, username);
        UserDO userDO = baseMapper.selectOne(queryWrapper);
        if (userDO == null) {
            throw new ClientException(BaseErrorCode.USER_NULL);
        }
        UserVO result = new UserVO();
        BeanUtils.copyProperties(userDO, result);
        return result;
    }

    @Override
    public Boolean existsByUsername(String username) {
        return userRegisterCachePenetrationBloomFilter.contains(username);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void register(UserRegisterReqDTO requestParam) {
        if (existsByUsername(requestParam.getUsername())) {
            throw new ClientException(USER_NAME_EXIST);
        }
        RLock lock = redissonClient.getLock(LOCK_USER_REGISTER_KEY + requestParam.getUsername());
        if (!lock.tryLock()) {
            throw new ClientException(USER_NAME_EXIST);
        }
        try {
            UserDO userDO = BeanUtil.toBean(requestParam, UserDO.class);
            userDO.setPassword(HashUtil.encryptByBcrypt(requestParam.getPassword()));
            int inserted = baseMapper.insert(userDO);
            if (inserted < 1) {
                throw new ClientException(USER_SAVE_ERROR);
            }
            groupService.saveGroup(requestParam.getUsername(), "默认分组");
            userRegisterCachePenetrationBloomFilter.add(requestParam.getUsername());
        } catch (DuplicateKeyException ex) {
            throw new ClientException(USER_EXIST);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void updateByUsername(UserUpdateReqDTO userUpdateReqDTO) {
        // 权限校验：仅允许当前登录用户修改自己的信息
        String currentUsername = UserContext.getUsername();
        if (currentUsername == null || !currentUsername.equals(userUpdateReqDTO.getUsername())) {
            throw new ClientException("无权修改其他用户的信息");
        }
        LambdaUpdateWrapper<UserDO> updateWrapper = Wrappers.lambdaUpdate(UserDO.class)
                .eq(UserDO::getUsername, userUpdateReqDTO.getUsername());
        UserDO userDO = BeanUtil.toBean(userUpdateReqDTO, UserDO.class);
        if (userDO.getPhone() != null && userDO.getPhone().contains("*")) {
            userDO.setPhone(null);
        }
        if (userUpdateReqDTO.getPassword() != null && !userUpdateReqDTO.getPassword().isEmpty()) {
            userDO.setPassword(HashUtil.encryptByBcrypt(userUpdateReqDTO.getPassword()));
        }
        baseMapper.update(userDO, updateWrapper);
    }

    @Override
    public UserLoginVO login(UserLoginReqDTO userLoginReqDTO) {
        LambdaQueryWrapper<UserDO> queryWrapper = Wrappers.lambdaQuery(UserDO.class)
                .eq(UserDO::getUsername, userLoginReqDTO.getUsername())
                .eq(UserDO::getDelFlag, 0);
        UserDO userDO = baseMapper.selectOne(queryWrapper);
        if (userDO == null) {
            throw new ClientException("用户不存在");
        }
        // BCrypt 密码校验
        if (!HashUtil.decryptByBcrypt(userLoginReqDTO.getPassword(), userDO.getPassword())) {
            throw new ClientException("密码错误");
        }
        Map<Object, Object> hasLoginMap = stringRedisTemplate.opsForHash().entries(USER_LOGIN_KEY + userLoginReqDTO.getUsername());
        if (CollUtil.isNotEmpty(hasLoginMap)) {
            // 续期旧 token 的会话映射
            String token = hasLoginMap.keySet().stream()
                    .findFirst()
                    .map(Object::toString)
                    .orElseThrow(() -> new ClientException("用户登录错误"));
            stringRedisTemplate.opsForValue().set(String.format(SESSION_KEY, token), userLoginReqDTO.getUsername(), sessionTtlMinutes, TimeUnit.MINUTES);
            stringRedisTemplate.expire(USER_LOGIN_KEY + userLoginReqDTO.getUsername(), sessionTtlMinutes, TimeUnit.MINUTES);
            // 刷新该用户 GID 正向索引集合 TTL（并补齐集合）
            refreshUserGidsIndex(userLoginReqDTO.getUsername());
            return new UserLoginVO(token);
        }
        // 生成新 token，并同时写入会话映射与兼容的用户名 Hash
        String uuid = UUID.randomUUID().toString();
        stringRedisTemplate.opsForValue().set(String.format(SESSION_KEY, uuid), userLoginReqDTO.getUsername(), sessionTtlMinutes, TimeUnit.MINUTES);
        stringRedisTemplate.opsForHash().put(USER_LOGIN_KEY + userLoginReqDTO.getUsername(), uuid, JSON.toJSONString(userDO));
        stringRedisTemplate.expire(USER_LOGIN_KEY + userLoginReqDTO.getUsername(), sessionTtlMinutes, TimeUnit.MINUTES);
        // 刷新该用户 GID 正向索引集合 TTL（并补齐集合）
        refreshUserGidsIndex(userLoginReqDTO.getUsername());
        return new UserLoginVO(uuid);
    }

    @Override
    public Boolean checkLogin(String username, String token) {
        String actualUsername = stringRedisTemplate.opsForValue().get(String.format(SESSION_KEY, token));
        return actualUsername != null && actualUsername.equals(username);
    }

    @Override
    public void logout(String username, String token) {
        // 幂等：即使 token 已过期或不存在也返回成功
        if (username == null || token == null) {
            return;
        }
        String key = String.format(SESSION_KEY, token);
        try {
            stringRedisTemplate.delete(key);
        } catch (Throwable t) {
            log.warn("Logout delete session error, username={}", username, t);
        }
        try {
            stringRedisTemplate.opsForHash().delete(USER_LOGIN_KEY + username, token);
        } catch (Throwable t) {
            log.warn("Logout delete token index error, username={}", username, t);
        }
    }

    /**
     * 刷新用户所有 gid 的反向索引 TTL（并纠偏 value）
     */
    private void refreshUserGidsIndex(String username) {
        LambdaQueryWrapper<GroupDO> queryWrapper = Wrappers.lambdaQuery(GroupDO.class)
                .eq(GroupDO::getUsername, username)
                .eq(GroupDO::getDelFlag, 0);
        try {
            List<GroupDO> groups = groupMapper.selectList(queryWrapper);
            String setKey = String.format(USER_GIDS_KEY, username);
            if (groups == null || groups.isEmpty()) {
                stringRedisTemplate.expire(setKey, sessionTtlMinutes, TimeUnit.MINUTES);
                return;
            }
            List<String> keys = Collections.singletonList(setKey);
            List<Object> args = new ArrayList<>();
            args.add(String.valueOf(sessionTtlMinutes * 60));
            for (GroupDO groupDO : groups) {
                args.add(groupDO.getGid());
            }
            stringRedisTemplate.execute(USER_GIDS_REFRESH_SCRIPT, keys, args.toArray());
        } catch (Throwable t) {
            // 可按需记录日志
            log.error("Refresh user_gids index error, username={}", username, t);
        }
    }
}
