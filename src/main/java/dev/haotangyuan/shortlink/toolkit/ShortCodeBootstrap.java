package dev.haotangyuan.shortlink.toolkit;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * 启动引导：由 Spring 容器在应用启动时执行一次，初始化短码生成器。
 *
 * @author: haotangyuan
 */
@Component
@RequiredArgsConstructor
public class ShortCodeBootstrap {

    private final StringRedisTemplate stringRedisTemplate;
    private final ShortCodeProps shortCodeProps;

    @PostConstruct
    public void init() {
        ShortCodeUtil.init(stringRedisTemplate, shortCodeProps);
    }
}

