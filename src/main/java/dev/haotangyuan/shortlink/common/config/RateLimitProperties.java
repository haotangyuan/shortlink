package dev.haotangyuan.shortlink.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 短链接系统限流配置文件
 *
 * @author: haotangyuan
 */
@Data
@Component
@ConfigurationProperties(prefix = "short-link.rate-limit")
public class RateLimitProperties {

    private DetailProperties create = new DetailProperties();
    private DetailProperties redirect = new DetailProperties();
    private DetailProperties stats = new DetailProperties();

    @Data
    public static class DetailProperties {

        /**
         * 是否开启限流
         */
        private Boolean enable = true;

        /**
         * 每秒钟允许通过的请求数
         */
        private double rps = 100.0;

        /**
         * 获取令牌超时时间，单位：毫秒
         */
        private int timeout = 100;
    }
}
