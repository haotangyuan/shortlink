package dev.haotangyuan.shortlink.mq.task;

import dev.haotangyuan.shortlink.mq.consumer.LinkStatsSaveConsumer;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.core.io.ClassPathResource;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static dev.haotangyuan.shortlink.common.constant.RedisKeyConstant.SHORT_LINK_STATS_STREAM_GROUP_KEY;
import static dev.haotangyuan.shortlink.common.constant.RedisKeyConstant.SHORT_LINK_STATS_STREAM_TOPIC_KEY;

/**
 * Pending 消息恢复任务（单线程定时巡检）
 *
 * @author: haotangyuan
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PendingMessageRecoveryTask {

    private final StringRedisTemplate stringRedisTemplate;
    private final LinkStatsSaveConsumer consumer;

    private DefaultRedisScript<List<?>> autoClaimScript;
    private static final String STREAM_XAUTOCLAIM_LUA_PATH = "lua/stream_xautoclaim_recover.lua";
    private static final String RECOVER_CURSOR_KEY = "short-link:stats-stream:recover-cursor";

    @PostConstruct
    public void initScript() {
        autoClaimScript = new DefaultRedisScript<>();
        autoClaimScript.setScriptSource(new ResourceScriptSource(new ClassPathResource(STREAM_XAUTOCLAIM_LUA_PATH)));
        autoClaimScript.setResultType((Class) List.class);
    }

    @Scheduled(fixedRate = 30000) // 每 30 秒检查一次
    public void recoverPendingMessages() {
        try {
            String startId = stringRedisTemplate.opsForValue().get(RECOVER_CURSOR_KEY);
            if (startId == null || startId.isBlank()) startId = "0-0";

            long minIdleMs = Duration.ofMinutes(2).toMillis();
            int count = 200;
            String consumerName = "stats-recoverer";

            List<?> res = stringRedisTemplate.execute(
                    autoClaimScript,
                    Collections.singletonList(SHORT_LINK_STATS_STREAM_TOPIC_KEY),
                    SHORT_LINK_STATS_STREAM_GROUP_KEY,
                    consumerName,
                    String.valueOf(minIdleMs),
                    startId,
                    String.valueOf(count)
            );

            if (res == null || res.size() < 2) {
                log.debug("PEL 巡检: 无消息需要补偿，跳过本轮");
                return;
            }
            String nextStart = deserializeString(res.get(0));
            List<?> entriesRaw = (List<?>) res.get(1);

            if (entriesRaw == null || entriesRaw.isEmpty()) {
                log.debug("PEL 巡检: 本轮未认领到消息，nextStart={}", nextStart);
                if (nextStart != null && !nextStart.isEmpty() && !nextStart.equals(startId)) {
                    stringRedisTemplate.opsForValue().set(RECOVER_CURSOR_KEY, nextStart);
                }
                return;
            }

            int recovered = 0;
            for (Object raw : entriesRaw) {
                if (!(raw instanceof List<?> item) || item.size() < 2) {
                    continue;
                }
                String id = deserializeString(item.get(0));
                List<?> flatFields = (List<?>) item.get(1);
                Map<String, String> map = convertFlatToMap(flatFields);

                try {
                    MapRecord<String, String, String> record = MapRecord.create(
                            SHORT_LINK_STATS_STREAM_TOPIC_KEY, map).withId(RecordId.of(id));
                    consumer.onMessage(record);
                    recovered++;
                } catch (Exception e) {
                    log.error("恢复 Pending 消息失败: {}", id, e);
                }
            }

            if (nextStart != null && !nextStart.isEmpty()) {
                stringRedisTemplate.opsForValue().set(RECOVER_CURSOR_KEY, nextStart);
            }
            log.info("PEL 巡检: 通过 XAUTOCLAIM 恢复 {} 条超时消息，下一起始点={}", recovered, nextStart);
        } catch (Exception e) {
            log.error("PEL 恢复任务执行失败", e);
        }
    }

    /**
     * 将 Redis Stream 字段值转换为 Map，输入格式为 [field1, value1, field2, value2, ...]
     */
    private Map<String, String> convertFlatToMap(List<?> kvs) {
        if (kvs == null || kvs.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, String> result = new LinkedHashMap<>(kvs.size() / 2 + 1);
        for (int i = 0; i + 1 < kvs.size(); i += 2) {
            String key = deserializeString(kvs.get(i));
            String val = deserializeString(kvs.get(i + 1));
            if (key != null && val != null) {
                result.put(key, val);
            }
        }
        return result;
    }

    private String deserializeString(Object obj) {
        if (obj == null) {
            return null;
        }
        if (obj instanceof String s) {
            return s;
        }
        if (obj instanceof byte[] bytes) {
            return stringRedisTemplate.getStringSerializer().deserialize(bytes);
        }
        return String.valueOf(obj);
    }
}

