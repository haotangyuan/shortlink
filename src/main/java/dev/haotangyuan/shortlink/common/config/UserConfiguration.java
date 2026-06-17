package dev.haotangyuan.shortlink.common.config;

import dev.haotangyuan.shortlink.common.web.ApiTokenAuthFilter;
import dev.haotangyuan.shortlink.common.web.UserFlowRiskControlFilter;
import dev.haotangyuan.shortlink.common.web.UserTransmitFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * 用户配置自动装配
 *
 * @author: haotangyuan
 */
@Configuration
public class UserConfiguration {

    /**
     * 用户信息传递过滤器
     */
    @Bean
    public FilterRegistrationBean<UserTransmitFilter> globalUserTransmitFilter(
            StringRedisTemplate stringRedisTemplate,
            @Value("${short-link.session-ttl-minutes:30}") int sessionTtlMinutes) {
        FilterRegistrationBean<UserTransmitFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new UserTransmitFilter(stringRedisTemplate, sessionTtlMinutes));
        // 仅拦截管理端路由，避免误伤核心与跳转
        registration.addUrlPatterns("/api/short-link/admin/*");
        registration.setOrder(0);
        return registration;
    }

    /**
     * Core API Token 过滤器
     */
    @Bean
    public FilterRegistrationBean<ApiTokenAuthFilter> coreApiTokenAuthFilter(StringRedisTemplate stringRedisTemplate) {
        FilterRegistrationBean<ApiTokenAuthFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new ApiTokenAuthFilter(stringRedisTemplate));
        registration.addUrlPatterns("/api/short-link/v1/*");
        registration.setOrder(1);
        return registration;
    }

    /**
     * 用户操作流量风控过滤器
     */
    @Bean
    @ConditionalOnProperty(name = "short-link.flow-limit.enable", havingValue = "true")
    public FilterRegistrationBean<UserFlowRiskControlFilter> globalUserFlowRiskControlFilter(
            StringRedisTemplate stringRedisTemplate,
            UserFlowRiskControlConfiguration userFlowRiskControlConfiguration) {
        FilterRegistrationBean<UserFlowRiskControlFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new UserFlowRiskControlFilter(stringRedisTemplate, userFlowRiskControlConfiguration));
        registration.addUrlPatterns("/api/short-link/*");
        registration.setOrder(10);
        return registration;
    }
}
