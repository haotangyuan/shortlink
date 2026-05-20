package dev.haotangyuan.shortlink.common.config;

import com.google.common.util.concurrent.RateLimiter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 短链接系统限流配置文件
 *
 * @author: haotangyuan
 */
@Configuration
public class RateLimitConfiguration {

    private RateLimiter unlimitedRateLimiter() {
        return RateLimiter.create(Double.MAX_VALUE);
    }

    @Bean("createRateLimiter")
    public RateLimiter createRateLimiter(RateLimitProperties p) {
        return Boolean.TRUE.equals(p.getCreate().getEnable())
                ? RateLimiter.create(p.getCreate().getRps())
                : unlimitedRateLimiter();
    }

    @Bean("redirectRateLimiter")
    public RateLimiter redirectRateLimiter(RateLimitProperties p) {
        return Boolean.TRUE.equals(p.getRedirect().getEnable())
                ? RateLimiter.create(p.getRedirect().getRps())
                : unlimitedRateLimiter();
    }

    @Bean("statsRateLimiter")
    public RateLimiter statsRateLimiter(RateLimitProperties p) {
        return Boolean.TRUE.equals(p.getStats().getEnable())
                ? RateLimiter.create(p.getStats().getRps())
                : unlimitedRateLimiter();
    }
}
