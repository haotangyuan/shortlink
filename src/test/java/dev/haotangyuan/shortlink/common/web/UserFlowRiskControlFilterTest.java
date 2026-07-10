package dev.haotangyuan.shortlink.common.web;

import dev.haotangyuan.shortlink.common.biz.user.UserContext;
import dev.haotangyuan.shortlink.common.config.UserFlowRiskControlConfiguration;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserFlowRiskControlFilterTest {

    @AfterEach
    void clearUserContext() {
        UserContext.removeUser();
    }

    @Test
    void readOnlyRequestsDoNotConsumeTheWriteQuota() throws Exception {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        UserFlowRiskControlFilter filter = new UserFlowRiskControlFilter(
                redisTemplate,
                mock(UserFlowRiskControlConfiguration.class)
        );
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);
        when(request.getMethod()).thenReturn("GET");
        UserContext.setUsername("alice");

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        verify(redisTemplate, never()).execute(any(), any(), any(Object[].class));
    }

    @Test
    void unauthenticatedWritesDoNotShareAGlobalPublicQuota() throws Exception {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        UserFlowRiskControlFilter filter = new UserFlowRiskControlFilter(
                redisTemplate,
                mock(UserFlowRiskControlConfiguration.class)
        );
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);
        when(request.getMethod()).thenReturn("POST");

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        verify(redisTemplate, never()).execute(any(), any(), any(Object[].class));
    }
}
