package dev.haotangyuan.shortlink.mq.idempotent;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

import static dev.haotangyuan.shortlink.common.constant.RedisKeyConstant.IDEMPOTENT_KEY_PREFIX;

/**
 * 消息队列幂等处理器
 * @author: haotangyuan
 */
@Component
@RequiredArgsConstructor
public class MessageQueueIdempotentHandler {

    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 判断消息是否已被处理过（同时尝试标记为处理中）
     * @param messageId 消息唯一标识
     * @return true-消息已被处理过（重复消费）, false-首次处理
     */
    public boolean isProcessed(String messageId) {
        String key = String.format(IDEMPOTENT_KEY_PREFIX, messageId);
        // setIfAbsent 返回 false 表示 key 已存在（已被处理过）
        return Boolean.FALSE.equals(stringRedisTemplate.opsForValue().setIfAbsent(key, "0", 2L, TimeUnit.MINUTES));
    }

    /**
     * 判断消息消费流程是否执行完成
     * @param messageId 消息唯一标识
     * @return 消息是否执行完成
     */
    public boolean isAccomplish(String messageId) {
        String key = String.format(IDEMPOTENT_KEY_PREFIX, messageId);
        return Objects.equals(stringRedisTemplate.opsForValue().get(key), "1");
    }

    /**
     * 设置消息流程执行完成
     * 第一次被获取到的最坏时间为 minIdleMs + 巡检间隔，也就是 2.5 分钟
     * @param messageId 消息唯一标识
     */
    public void setAccomplish(String messageId) {
        String key = String.format(IDEMPOTENT_KEY_PREFIX, messageId);
        stringRedisTemplate.opsForValue().set(key, "1", 4L, TimeUnit.MINUTES);
    }

    /**
     * 释放幂等标识（消息处理失败时调用）
     * @param messageId 消息唯一标识
     */
    public void release(String messageId) {
        String key = String.format(IDEMPOTENT_KEY_PREFIX, messageId);
        stringRedisTemplate.delete(key);
    }
}