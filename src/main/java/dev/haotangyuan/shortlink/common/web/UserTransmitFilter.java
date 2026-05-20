package dev.haotangyuan.shortlink.common.web;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import dev.haotangyuan.shortlink.common.biz.user.UserContext;
import dev.haotangyuan.shortlink.common.convention.exception.ClientException;
import dev.haotangyuan.shortlink.common.convention.result.Results;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.io.IOException;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static dev.haotangyuan.shortlink.common.constant.RedisKeyConstant.SESSION_KEY;
import static dev.haotangyuan.shortlink.common.constant.RedisKeyConstant.USER_GIDS_KEY;
import static dev.haotangyuan.shortlink.common.constant.UserConstant.PUBLIC_USERNAME;
import static dev.haotangyuan.shortlink.common.convention.errorcode.BaseErrorCode.USER_TOKEN_FAIL;

/**
 * @author: haotangyuan
 */
@RequiredArgsConstructor
@Slf4j
public class UserTransmitFilter implements Filter {

    private final StringRedisTemplate stringRedisTemplate;

    private final Set<String> IGNORE_URI = Set.of(
            "/api/short-link/admin/v1/user/login",
            "/api/short-link/admin/v1/user/exists"
    );

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest httpServletRequest = (HttpServletRequest) servletRequest;
        String requestURI = httpServletRequest.getRequestURI();
        if (!IGNORE_URI.contains(requestURI)) {
            String method = httpServletRequest.getMethod();
            boolean isRegister = Objects.equals(requestURI, "/api/short-link/admin/v1/user") && Objects.equals(method, "POST");
            if (!isRegister) {
                String authz = httpServletRequest.getHeader("Authorization");
                boolean adminCreate = isAdminCreate(httpServletRequest);
                // 特殊放行：admin 创建短链允许未携带 Authorization，以 public 身份创建
                if (adminCreate && StrUtil.isBlank(authz)) {
                    UserContext.setUsername(PUBLIC_USERNAME);
                } else {
                    if (StrUtil.isBlank(authz) || !StrUtil.startWithIgnoreCase(authz, "Bearer ")) {
                        unauthorized((HttpServletResponse) servletResponse);
                        return;
                    }
                    String token = StrUtil.trim(authz.substring(7));
                    if (StrUtil.isBlank(token)) {
                        unauthorized((HttpServletResponse) servletResponse);
                        return;
                    }
                    String username;
                    try {
                        String key = String.format(SESSION_KEY, token);
                        username = stringRedisTemplate.opsForValue().get(key);
                        if (StrUtil.isBlank(username)) {
                            unauthorized((HttpServletResponse) servletResponse);
                            return;
                        }
                        // 仅会话续期，不对 gid 索引续期
                        stringRedisTemplate.expire(key, 30, TimeUnit.MINUTES);
                        // 仅刷新该用户 GID 正向索引集合 TTL（不回库、不补全）
                        expireUserGidsTTL(username);
                    } catch (Throwable t) {
                        log.error("Admin session validate error", t);
                        unauthorized((HttpServletResponse) servletResponse);
                        return;
                    }
                    // 仅设置 username 即可
                    UserContext.setUsername(username);
                }
            }
        }

        try {
            filterChain.doFilter(servletRequest, servletResponse);
        } finally {
            UserContext.removeUser();
        }
    }

    private void expireUserGidsTTL(String username) {
        String setKey = String.format(USER_GIDS_KEY, username);
        try {
            stringRedisTemplate.expire(setKey, 30, java.util.concurrent.TimeUnit.MINUTES);
        } catch (Throwable t) {
            log.error("Expire user_gids TTL error, username={}", username, t);
        }
    }

    private void unauthorized(HttpServletResponse resp) throws java.io.IOException {
        resp.setStatus(401);
        resp.setHeader("WWW-Authenticate", "Bearer");
        resp.setContentType("application/json;charset=UTF-8");
        Object body = JSON.toJSONString(Results.failure(new ClientException(USER_TOKEN_FAIL)));
        resp.getWriter().print(body);
    }

    private boolean isAdminCreate(HttpServletRequest req) {
        return "POST".equalsIgnoreCase(req.getMethod()) && "/api/short-link/admin/v1/create".equals(req.getRequestURI());
    }
}
