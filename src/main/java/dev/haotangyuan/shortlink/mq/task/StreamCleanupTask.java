package dev.haotangyuan.shortlink.mq.task;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import static dev.haotangyuan.shortlink.common.constant.RedisKeyConstant.SHORT_LINK_STATS_STREAM_TOPIC_KEY;

import java.util.Collections;

import static dev.haotangyuan.shortlink.common.constant.RedisKeyConstant.SHORT_LINK_STATS_STREAM_GROUP_KEY;

/**
 * Stream 消息清理任务
 * 策略：保留 Pending 消息 + 最近已消费的缓冲，删除其他旧消息
 *
 * @author: haotangyuan
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StreamCleanupTask {

    private final StringRedisTemplate stringRedisTemplate;

    private static final long CONSUMED_BUFFER_SIZE = 3000; // 保留已消费缓冲

    private DefaultRedisScript<Long> trimScript;

    private static final String STREAM_TRIM_MINID_LUA_PATH = "lua/stream_trim_minid.lua";

    @PostConstruct
    public void initTrimScript() {
        this.trimScript = new DefaultRedisScript<>();
        this.trimScript.setResultType(Long.class);
        this.trimScript.setScriptSource(new ResourceScriptSource(new ClassPathResource(STREAM_TRIM_MINID_LUA_PATH)));
    }

    @Scheduled(fixedRate = 300_000)
    public void cleanupConsumedMessages() {
        try {
            Long trimmed = stringRedisTemplate.execute(
                    trimScript,
                    Collections.singletonList(SHORT_LINK_STATS_STREAM_TOPIC_KEY),
                    SHORT_LINK_STATS_STREAM_GROUP_KEY,
                    String.valueOf(CONSUMED_BUFFER_SIZE)
            );
            if (trimmed != null && trimmed > 0) {
                log.info("Stream 清理成功: 删除 {} 条消息", trimmed);
            } else {
                log.debug("Stream 清理执行: 本轮无可删消息");
            }
        } catch (Exception e) {
            log.error("Stream 清理失败", e);
        }
    }
}
