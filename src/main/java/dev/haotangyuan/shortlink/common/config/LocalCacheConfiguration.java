package dev.haotangyuan.shortlink.common.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 本地缓存配置
 * 包含短链接跳转相关的本地缓存：
 * 1. redirectLockCache: 本地每键互斥锁，避免跳转热点使用分布式锁导致尾延迟放大
 * 2. redirectCache: 短链接跳转目标 URL 缓存，减少 Redis 网络往返
 */
@Configuration
public class LocalCacheConfiguration {

    /**
     * 跳转路径本地互斥锁缓存
     * key: fullShortUrl, value: ReentrantLock
     * 设计目标：避免跳转热点 Key 的分布式锁放大故障域与尾延迟
     */
    @Bean(name = "redirectLockCache")
    public Cache<String, ReentrantLock> redirectLockCache() {
        return Caffeine.newBuilder()
                .expireAfterAccess(Duration.ofSeconds(60))
                .initialCapacity(1024)
                .maximumSize(8096)
                .recordStats()
                .build();
    }

    /**
     * 短链接跳转目标 URL 本地缓存
     * key: fullShortUrl, value: originUrl
     * 设计目标：减少 Redis 网络往返，提升热点短链接跳转性能
     */
    @Bean(name = "redirectCache")
    public Cache<String, String> redirectCache() {
        return Caffeine.newBuilder()
                .maximumSize(10_000)
                .expireAfterAccess(Duration.ofMinutes(5))
                .recordStats()
                .build();
    }
}
