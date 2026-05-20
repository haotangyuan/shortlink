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

import static dev.haotangyuan.shortlink.common.constant.RedisKeyConstant.API_TOKEN_HASH_KEY;
import static dev.haotangyuan.shortlink.common.constant.UserConstant.PUBLIC_USERNAME;
import static dev.haotangyuan.shortlink.common.convention.errorcode.BaseErrorCode.USER_TOKEN_FAIL;

/**
 * Core API Token 鉴权过滤器：拦截 /api/short-link/v1/*
 * 校验 Authorization: Bearer {token}，通过 TokenService 解析 username
 * 仅设置 UserContext 的 username 字段
 *
 * @author: haotangyuan
 */
@RequiredArgsConstructor
@Slf4j
public class ApiTokenAuthFilter implements Filter {

    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;
        // 解析 Authorization: Bearer {token}
        String authz = req.getHeader("Authorization");
        boolean hasAuthz = StrUtil.isNotBlank(authz);
        boolean bearer = hasAuthz && StrUtil.startWithIgnoreCase(authz, "Bearer ");

        // 仅对创建接口放行未携带 Authorization 的请求，并将其标记为 public 用户
        if (!hasAuthz && isCreatePath(req)) {
            try {
                UserContext.setUsername(PUBLIC_USERNAME);
                chain.doFilter(request, response);
            } finally {
                UserContext.removeUser();
            }
            return;
        }

        // 其它情况必须是 Bearer，并且可解析到有效用户名
        if (!bearer) {
            unauthorized(resp);
            return;
        }
        String token = StrUtil.trim(authz.substring(7));
        if (StrUtil.isBlank(token)) {
            unauthorized(resp);
            return;
        }
        String username = null;
        try {
            String tokenHash = sha256Hex(token);
            String key = String.format(API_TOKEN_HASH_KEY, tokenHash);
            username = stringRedisTemplate.opsForValue().get(key);
        } catch (Throwable t) {
            log.error("Read api token mapping error", t);
        }
        if (StrUtil.isBlank(username)) {
            unauthorized(resp);
            return;
        }
        try {
            UserContext.setUsername(username);
            chain.doFilter(request, response);
        } finally {
            UserContext.removeUser();
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
            throw new ClientException(USER_TOKEN_FAIL);
        }
    }

    private boolean isCreatePath(HttpServletRequest req) {
        return "POST".equalsIgnoreCase(req.getMethod()) && "/api/short-link/v1/create".equals(req.getRequestURI());
    }

    private void unauthorized(HttpServletResponse resp) throws IOException {
        resp.setStatus(401);
        resp.setHeader("WWW-Authenticate", "Bearer");
        resp.setContentType("application/json;charset=UTF-8");
        Object body = JSON.toJSONString(Results.failure(new ClientException(USER_TOKEN_FAIL)));
        resp.getWriter().print(body);
    }
}
