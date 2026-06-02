package dev.haotangyuan.shortlink.common.config;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

@Configuration
public class FrontendSpaConfiguration {

    @Bean
    public FilterRegistrationBean<Filter> frontendSpaForwardFilter() {
        FilterRegistrationBean<Filter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new FrontendSpaForwardFilter());
        registration.addUrlPatterns("/app", "/app/*");
        return registration;
    }

    static class FrontendSpaForwardFilter implements Filter {

        @Override
        public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
                throws IOException, ServletException {
            HttpServletRequest httpRequest = (HttpServletRequest) request;
            String path = httpRequest.getRequestURI().substring(httpRequest.getContextPath().length());
            if (isSpaRoute(path)) {
                request.getRequestDispatcher("/app/index.html").forward(request, response);
                return;
            }
            chain.doFilter(request, response);
        }

        private boolean isSpaRoute(String path) {
            return path.equals("/app")
                    || path.equals("/app/")
                    || path.startsWith("/app/") && !lastSegment(path).contains(".");
        }

        private String lastSegment(String path) {
            int index = path.lastIndexOf('/');
            return index < 0 ? path : path.substring(index + 1);
        }
    }
}
