package dev.haotangyuan.shortlink.common.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.util.concurrent.RateLimiter;
import dev.haotangyuan.shortlink.common.config.RateLimitProperties;
import dev.haotangyuan.shortlink.common.convention.result.Results;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * 短链接系统限流过滤器
 *
 * @author: haotangyuan
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
@Component
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Pattern SHORT_URI_PATH = Pattern.compile("^/[A-Za-z0-9]{1,8}$");

    @Qualifier("createRateLimiter")
    private final RateLimiter createRateLimiter;
    @Qualifier("redirectRateLimiter")
    private final RateLimiter redirectRateLimiter;
    @Qualifier("statsRateLimiter")
    private final RateLimiter statsRateLimiter;
    private final RateLimitProperties props;
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Override
    protected boolean shouldNotFilterAsyncDispatch() {
        return true;
    } // 异步阶段不再执行

    @Override
    protected boolean shouldNotFilterErrorDispatch() {
        return true;
    } // 错误分派不再执行

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse resp, FilterChain filterChain)
            throws IOException, ServletException {

        String path = req.getRequestURI();

        if (isRedirectPath(req)) {
            if (!redirectRateLimiter.tryAcquire(1, props.getRedirect().getTimeout(), TimeUnit.MILLISECONDS)) {
                tooMany(req, resp);
                return;
            }
        } else {
            int permitCount = creationPermitCount(path);
            if (permitCount > 0) {
                if (!createRateLimiter.tryAcquire(permitCount, props.getCreate().getTimeout(), TimeUnit.MILLISECONDS)) {
                    tooMany(req, resp);
                    return;
                }
            } else if (path.startsWith("/api/short-link/admin/v1/stats")) {
                if (!statsRateLimiter.tryAcquire(1, props.getStats().getTimeout(), TimeUnit.MILLISECONDS)) {
                    tooMany(req, resp);
                    return;
                }
            }
        }
        filterChain.doFilter(req, resp);
    }

    private int creationPermitCount(String path) {
        return switch (path) {
            case "/api/short-link/v1/create", "/api/short-link/admin/v1/create" -> 1;
            case "/api/short-link/v1/create/batch", "/api/short-link/admin/v1/create/batch" -> 5;
            default -> 0;
        };
    }

    private boolean isRedirectPath(HttpServletRequest req) {
        if (!"GET".equalsIgnoreCase(req.getMethod())) return false;
        String path = req.getRequestURI();
        return SHORT_URI_PATH.matcher(path).matches();
    }

    private void tooMany(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setStatus(429);
        if (isRedirectPath(req)) {
            resp.setContentType("text/html;charset=UTF-8");
            String src = URLEncoder.encode(req.getRequestURI() + (req.getQueryString() != null ? "?" + req.getQueryString() : ""), StandardCharsets.UTF_8);
            resp.getWriter().write(String.format("""
                    <!doctype html><meta charset="utf-8">
                    <title>Too Many Requests</title>
                    <meta name="robots" content="noindex,nofollow">
                    <style>body{font-family:system-ui;margin:6vh auto;max-width:600px;padding:0 16px;line-height:1.6}button{padding:.6em 1em;border:0;border-radius:10px;cursor:pointer}</style>
                    <h1>访问太频繁</h1>
                    <p>当前访问人数较多，请稍后再试。</p>
                    <p>
                      <button id="retry">立即重试</button>
                      <button id="copy">复制短链</button>
                    </p>
                    <script>
                      const src = decodeURIComponent('%s');
                      document.getElementById('retry').onclick = () => location.href = src;
                      document.getElementById('copy').onclick = async () => { try { await navigator.clipboard.writeText(location.origin + src); alert('已复制'); } catch(e){ alert('复制失败'); } }
                    </script>
                    """, src)
            );
        } else {
            resp.setContentType("application/json;charset=UTF-8");
            Object body = Results.failure("B100000", "当前流量较高，请稍后再试...");
            resp.getWriter().write(OBJECT_MAPPER.writeValueAsString(body));
        }
    }
}
