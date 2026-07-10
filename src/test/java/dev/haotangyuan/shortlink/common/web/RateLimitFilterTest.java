package dev.haotangyuan.shortlink.common.web;

import com.google.common.util.concurrent.RateLimiter;
import dev.haotangyuan.shortlink.common.config.RateLimitProperties;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class RateLimitFilterTest {

    private final RateLimiter createRateLimiter = mock(RateLimiter.class);
    private final RateLimiter redirectRateLimiter = mock(RateLimiter.class);
    private final RateLimiter statsRateLimiter = mock(RateLimiter.class);
    private final FilterChain filterChain = mock(FilterChain.class);

    private RateLimitFilter filter;

    @BeforeEach
    void setUp() {
        RateLimitProperties properties = new RateLimitProperties();
        properties.getCreate().setTimeout(17);
        properties.getRedirect().setTimeout(19);
        properties.getStats().setTimeout(23);
        filter = new RateLimitFilter(createRateLimiter, redirectRateLimiter, statsRateLimiter, properties);
    }

    @Test
    void appliesExistingPermitCountsToAllCreationPaths() throws Exception {
        when(createRateLimiter.tryAcquire(anyInt(), anyLong(), eq(TimeUnit.MILLISECONDS))).thenReturn(true);

        for (String path : List.of(
                "/api/short-link/v1/create",
                "/api/short-link/admin/v1/create",
                "/api/short-link/v1/create/batch",
                "/api/short-link/admin/v1/create/batch")) {
            filter.doFilterInternal(postRequest(path), new MockHttpServletResponse(), filterChain);
        }

        verify(createRateLimiter, times(2)).tryAcquire(1, 17, TimeUnit.MILLISECONDS);
        verify(createRateLimiter, times(2)).tryAcquire(5, 17, TimeUnit.MILLISECONDS);
        verify(filterChain, times(4)).doFilter(any(), any());
        verifyNoInteractions(redirectRateLimiter, statsRateLimiter);
    }

    @Test
    void usesDedicatedLimitersForRedirectAndStatisticsPaths() throws Exception {
        when(redirectRateLimiter.tryAcquire(anyInt(), anyLong(), eq(TimeUnit.MILLISECONDS))).thenReturn(true);
        when(statsRateLimiter.tryAcquire(anyInt(), anyLong(), eq(TimeUnit.MILLISECONDS))).thenReturn(true);

        filter.doFilterInternal(getRequest("/abc123"), new MockHttpServletResponse(), filterChain);
        filter.doFilterInternal(getRequest("/api/short-link/admin/v1/stats"), new MockHttpServletResponse(), filterChain);

        verify(redirectRateLimiter).tryAcquire(1, 19, TimeUnit.MILLISECONDS);
        verify(statsRateLimiter).tryAcquire(1, 23, TimeUnit.MILLISECONDS);
        verify(filterChain, times(2)).doFilter(any(), any());
        verifyNoInteractions(createRateLimiter);
    }

    @Test
    void returnsTooManyRequestsWithoutCallingTheChainWhenCreationLimitRejects() throws Exception {
        when(createRateLimiter.tryAcquire(1, 17, TimeUnit.MILLISECONDS)).thenReturn(false);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(postRequest("/api/short-link/v1/create"), response, filterChain);

        assertEquals(429, response.getStatus());
        verifyNoInteractions(filterChain, redirectRateLimiter, statsRateLimiter);
    }

    private MockHttpServletRequest getRequest(String path) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", path);
        request.setRequestURI(path);
        return request;
    }

    private MockHttpServletRequest postRequest(String path) {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", path);
        request.setRequestURI(path);
        return request;
    }
}
