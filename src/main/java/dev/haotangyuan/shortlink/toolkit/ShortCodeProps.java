package dev.haotangyuan.shortlink.toolkit;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 短码生成器配置（application.yaml 可直接配置）
 * @author: haotangyuan
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "short-link.shortcode")
public class ShortCodeProps {

    /**
     * 固定短码长度（默认 6）
     */
    private Integer length = 6;

    /**
     *号段步长：每次从 Redis 预取的数量（默认 10000）
     */
    private Long segmentStep = 10_000L;

    /**
     * 预取阈值占比：剩余量 <= step*ratio 时触发预取（默认 0.2）
     */
    private Double prefetchRatio = 0.2d;

    /**
     * 仿射置换参数 a（需与 62 与 31 互素，且为奇数）。为空则用默认值
     */
    private String a;

    /**
     * 仿射置换参数 b（需 0<=b<62^length）。为空则用默认值
     */
    private String b;
}
