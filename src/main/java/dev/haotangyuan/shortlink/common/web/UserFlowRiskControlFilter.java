package dev.haotangyuan.shortlink.common.web;

import com.alibaba.fastjson2.JSON;
import com.google.common.collect.Lists;
import dev.haotangyuan.shortlink.common.biz.user.UserContext;
import dev.haotangyuan.shortlink.common.config.UserFlowRiskControlConfiguration;
import dev.haotangyuan.shortlink.common.convention.exception.ClientException;
import dev.haotangyuan.shortlink.common.convention.result.Results;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;

import java.io.IOException;
import java.io.PrintWriter;

import static dev.haotangyuan.shortlink.common.constant.UserConstant.PUBLIC_USERNAME;
import static dev.haotangyuan.shortlink.common.convention.errorcode.BaseErrorCode.FLOW_LIMIT_ERROR;

/**
 * 用户操作流量风控过滤器
 *
 * @author: haotangyuan
 */
@Slf4j
@RequiredArgsConstructor
public class UserFlowRiskControlFilter implements Filter {

    private final StringRedisTemplate stringRedisTemplate;
    private final UserFlowRiskControlConfiguration userFlowRiskControlConfiguration;

    private static final String USER_FLOW_RISK_CONTROL_LUA_SCRIPT_PATH = "lua/user_flow_risk_control.lua";
    private static final DefaultRedisScript<Long> REDIS_SCRIPT;

    static {
        REDIS_SCRIPT = new DefaultRedisScript<>();
        REDIS_SCRIPT.setScriptSource(new ResourceScriptSource(new ClassPathResource(USER_FLOW_RISK_CONTROL_LUA_SCRIPT_PATH)));
        REDIS_SCRIPT.setResultType(Long.class);
    }

    @SneakyThrows
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain) throws IOException, ServletException {
        String username = UserContext.getUsername();
        String method = ((HttpServletRequest) request).getMethod();
        if (isReadOnly(method) || username == null || PUBLIC_USERNAME.equals(username)) {
            filterChain.doFilter(request, response);
            return;
        }
        Long result;
        try {
            result = stringRedisTemplate.execute(REDIS_SCRIPT, Lists.newArrayList(username), String.valueOf(userFlowRiskControlConfiguration.getTimeWindow()));
        } catch (Throwable ex) {
            log.error("执行用户请求流量限制LUA脚本出错", ex);
            tooMany((HttpServletResponse) response);
            return;
        }
        if (result == null || result > userFlowRiskControlConfiguration.getMaxAccessCount()) {
            tooMany((HttpServletResponse) response);
            return;
        }
        filterChain.doFilter(request, response);
    }

    private boolean isReadOnly(String method) {
        return "GET".equalsIgnoreCase(method)
                || "HEAD".equalsIgnoreCase(method)
                || "OPTIONS".equalsIgnoreCase(method);
    }

    private void tooMany(HttpServletResponse resp) throws IOException {
        // 用户级限流：返回 429，携带 JSON 错误体
        resp.setStatus(429);
        resp.setCharacterEncoding("UTF-8");
        resp.setContentType("application/json;charset=UTF-8");
        String body = JSON.toJSONString(Results.failure(new ClientException(FLOW_LIMIT_ERROR)));
        try (PrintWriter writer = resp.getWriter()) {
            writer.print(body);
        }
    }
}
