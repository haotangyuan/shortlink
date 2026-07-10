package dev.haotangyuan.shortlink.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import dev.haotangyuan.shortlink.common.biz.user.UserContext;
import dev.haotangyuan.shortlink.common.convention.exception.ClientException;
import dev.haotangyuan.shortlink.dao.entity.TokenDO;
import dev.haotangyuan.shortlink.dao.mapper.TokenMapper;
import dev.haotangyuan.shortlink.dto.req.TokenCreateReqDTO;
import dev.haotangyuan.shortlink.vo.TokenVO;
import dev.haotangyuan.shortlink.service.TokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import static dev.haotangyuan.shortlink.common.constant.RedisKeyConstant.API_TOKEN_HASH_KEY;

@Slf4j
@Service
@RequiredArgsConstructor
public class TokenServiceImpl extends ServiceImpl<TokenMapper, TokenDO> implements TokenService {

    private static final int MAX_TOKEN_COUNT = 20;

    private final StringRedisTemplate stringRedisTemplate;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String createToken(TokenCreateReqDTO req) {
        String username = Objects.requireNonNull(UserContext.getUsername(), "用户未登录");
        validateCreateRequest(req);
        long tokenCount = baseMapper.selectCount(Wrappers.lambdaQuery(TokenDO.class)
                .eq(TokenDO::getUsername, username)
                .eq(TokenDO::getDelFlag, 0));
        if (tokenCount >= MAX_TOKEN_COUNT) {
            throw new ClientException("每个用户最多创建 " + MAX_TOKEN_COUNT + " 个令牌");
        }
        String token = java.util.UUID.randomUUID().toString().replace("-", "");
        String tokenHash = sha256Hex(token);
        String last4 = token.substring(Math.max(0, token.length() - 4));
        TokenDO entity = TokenDO.builder()
                .username(username)
                .tokenHash(tokenHash)
                .tokenLast4(last4)
                .name(StrUtil.blankToDefault(req.getName(), "默认令牌"))
                .enableStatus(0)
                .validDate(req.getValidDate())
                .describe(req.getDescribe())
                .build();
        if (baseMapper.insert(entity) < 1) {
            throw new ClientException("创建令牌失败");
        }
        writeRedisMapping(tokenHash, username, req.getValidDate());
        return token;
    }

    @Override
    public List<TokenVO> listTokens() {
        String username = Objects.requireNonNull(UserContext.getUsername(), "用户未登录");
        LambdaQueryWrapper<TokenDO> qw = Wrappers.lambdaQuery(TokenDO.class)
                .eq(TokenDO::getUsername, username)
                .eq(TokenDO::getDelFlag, 0)
                .orderByDesc(TokenDO::getUpdateTime);
        List<TokenDO> list = baseMapper.selectList(qw);
        return list.stream().map(each -> TokenVO.builder()
                .id(each.getId())
                .name(each.getName())
                .enableStatus(each.getEnableStatus())
                .validDate(each.getValidDate())
                .describe(each.getDescribe())
                .updateTime(each.getUpdateTime())
                .tokenMasked(mask(each.getTokenLast4()))
                .build()).toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteToken(Long id) {
        String username = Objects.requireNonNull(UserContext.getUsername(), "用户未登录");
        TokenDO token = baseMapper.selectById(id);
        if (token == null || !Objects.equals(token.getUsername(), username) || token.getDelFlag() != 0) {
            throw new ClientException("令牌不存在");
        }
        deleteRedisMapping(token.getTokenHash());
        token.setDelFlag(1);
        if (baseMapper.updateById(token) < 1) {
            throw new ClientException("删除令牌失败");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long id, Boolean enable) {
        String username = Objects.requireNonNull(UserContext.getUsername(), "用户未登录");
        TokenDO token = baseMapper.selectById(id);
        if (token == null || !Objects.equals(token.getUsername(), username) || token.getDelFlag() != 0) {
            throw new ClientException("令牌不存在");
        }
        if (Boolean.TRUE.equals(enable)) {
            if (token.getValidDate() != null && token.getValidDate().getTime() <= System.currentTimeMillis()) {
                throw new ClientException("令牌已过期");
            }
            token.setEnableStatus(0);
            if (baseMapper.updateById(token) < 1) {
                throw new ClientException("更新令牌失败");
            }
            writeRedisMapping(token.getTokenHash(), username, token.getValidDate());
        } else {
            deleteRedisMapping(token.getTokenHash());
            token.setEnableStatus(1);
            if (baseMapper.updateById(token) < 1) {
                throw new ClientException("更新令牌失败");
            }
        }
    }

    private void deleteRedisMapping(String tokenHash) {
        try {
            stringRedisTemplate.delete(String.format(API_TOKEN_HASH_KEY, tokenHash));
        } catch (Exception e) {
            log.error("Delete api-token mapping error", e);
            throw new ClientException("令牌吊销失败");
        }
    }

    private void writeRedisMapping(String tokenHash, String username, java.util.Date validDate) {
        String key = String.format(API_TOKEN_HASH_KEY, tokenHash);
        try {
            if (validDate == null) {
                stringRedisTemplate.opsForValue().set(key, username);
            } else {
                long ttl = validDate.getTime() - System.currentTimeMillis();
                if (ttl <= 0) throw new ClientException("令牌过期时间无效");
                stringRedisTemplate.opsForValue().set(key, username, ttl, TimeUnit.MILLISECONDS);
            }
        } catch (ClientException e) {
            throw e;
        } catch (Exception e) {
            log.error("Write api-token mapping error", e);
            throw new ClientException("令牌写入失败");
        }
    }

    private String mask(String last4) {
        if (StrUtil.isBlank(last4)) return "";
        return "****" + last4;
    }

    private void validateCreateRequest(TokenCreateReqDTO req) {
        if (req == null) {
            throw new ClientException("令牌参数不能为空");
        }
        if (req.getName() != null && req.getName().length() > 128) {
            throw new ClientException("令牌名称过长");
        }
        if (req.getDescribe() != null && req.getDescribe().length() > 255) {
            throw new ClientException("令牌描述过长");
        }
        if (req.getValidDate() != null && !req.getValidDate().after(new java.util.Date())) {
            throw new ClientException("令牌过期时间无效");
        }
    }

    private String sha256Hex(String s) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            byte[] bytes = md.digest(s.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new ClientException("生成令牌失败");
        }
    }
}
