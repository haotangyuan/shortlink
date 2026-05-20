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

import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import static dev.haotangyuan.shortlink.common.constant.RedisKeyConstant.API_TOKEN_HASH_KEY;

@Slf4j
@Service
@RequiredArgsConstructor
public class TokenServiceImpl extends ServiceImpl<TokenMapper, TokenDO> implements TokenService {

    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public String createToken(TokenCreateReqDTO req) {
        String username = Objects.requireNonNull(UserContext.getUsername(), "用户未登录");
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
        baseMapper.insert(entity);
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
                .name(each.getName())
                .enableStatus(each.getEnableStatus())
                .validDate(each.getValidDate())
                .describe(each.getDescribe())
                .tokenMasked(mask(each.getTokenLast4()))
                .build()).toList();
    }

    @Override
    public void deleteToken(Long id) {
        String username = Objects.requireNonNull(UserContext.getUsername(), "用户未登录");
        TokenDO token = baseMapper.selectById(id);
        if (token == null || !Objects.equals(token.getUsername(), username) || token.getDelFlag() != 0) {
            throw new ClientException("令牌不存在");
        }
        // 删除 Redis 映射（按哈希键）
        try {
            stringRedisTemplate.delete(String.format(API_TOKEN_HASH_KEY, token.getTokenHash()));
        } catch (Throwable t) {
            log.error("Delete api-token mapping error", t);
        }
        // 逻辑删除
        token.setDelFlag(1);
        baseMapper.updateById(token);
    }

    @Override
    public void updateStatus(Long id, Boolean enable) {
        String username = Objects.requireNonNull(UserContext.getUsername(), "用户未登录");
        TokenDO token = baseMapper.selectById(id);
        if (token == null || !Objects.equals(token.getUsername(), username) || token.getDelFlag() != 0) {
            throw new ClientException("令牌不存在");
        }
        if (Boolean.TRUE.equals(enable)) {
            // 启用：写入 Redis，若已过期则报错
            if (token.getValidDate() != null && token.getValidDate().getTime() <= System.currentTimeMillis()) {
                throw new ClientException("令牌已过期");
            }
            token.setEnableStatus(0);
            baseMapper.updateById(token);
            writeRedisMapping(token.getTokenHash(), username, token.getValidDate());
        } else {
            token.setEnableStatus(1);
            baseMapper.updateById(token);
            try {
                stringRedisTemplate.delete(String.format(API_TOKEN_HASH_KEY, token.getTokenHash()));
            } catch (Exception e) {
                log.error("Delete api-token mapping error", e);
            }
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
