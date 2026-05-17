package dev.haotangyuan.shortlink.mq.consumer;

import com.alibaba.fastjson2.JSON;
import dev.haotangyuan.shortlink.common.convention.exception.ServiceException;
import dev.haotangyuan.shortlink.dto.biz.LinkStatsRecordDTO;
import dev.haotangyuan.shortlink.mq.idempotent.MessageQueueIdempotentHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.stream.StreamListener;
import org.springframework.stereotype.Component;

import java.util.Map;

import static dev.haotangyuan.shortlink.common.constant.RedisKeyConstant.*;

/**
 * 短链接监控状态保存消息队列消费者
 * @author: haotangyuan
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LinkStatsSaveConsumer implements StreamListener<String, MapRecord<String, String, String>> {

    private final StringRedisTemplate stringRedisTemplate;
    private final MessageQueueIdempotentHandler messageQueueIdempotentHandler;
    private final LinkStatsSaver linkStatsSaver;

    @Override
    public void onMessage(MapRecord<String, String, String> message) {
        RecordId id = message.getId();

        // 幂等检查
        if (messageQueueIdempotentHandler.isProcessed(id.toString())) {
            // 消息已被处理过（重复消费）
            if (messageQueueIdempotentHandler.isAccomplish(id.toString())) {
                // 已完成，补偿 ACK
                try {
                    stringRedisTemplate.opsForStream().acknowledge(
                        SHORT_LINK_STATS_STREAM_TOPIC_KEY,
                        SHORT_LINK_STATS_STREAM_GROUP_KEY,
                        id
                    );
                } catch (Exception e) {
                    log.warn("补偿 ACK 失败: {}", id, e);
                }
                return;
            }
            // 正在处理中，抛异常让消息留在 Pending
            throw new ServiceException("消息未完成流程，需要消息队列重试");
        }

        try {
            // 业务逻辑
            Map<String, String> producerMap = message.getValue();
            LinkStatsRecordDTO statsRecord = JSON.parseObject(producerMap.get("statsRecord"), LinkStatsRecordDTO.class);
            linkStatsSaver.save(statsRecord, id.toString());

        } catch (DuplicateKeyException ex) {
            // messageId 唯一索引冲突 = 已处理过，直接视为成功
            log.info("Message already processed (DB duplicate key), skip: {}", id);
        } catch (Throwable ex) {
            // 业务失败，删除幂等标记，不 ACK
            messageQueueIdempotentHandler.release(id.toString());
            log.error("业务逻辑执行失败，消息将重试: {}", id, ex);
            throw ex;
        }

        // 业务成功后，标记完成并 ACK
        try {
            messageQueueIdempotentHandler.setAccomplish(id.toString());
        } catch (Exception e) {
            log.error("设置幂等标记失败，但业务已成功，继续 ACK: {}", id, e);
        }

        try {
            stringRedisTemplate.opsForStream().acknowledge(
                SHORT_LINK_STATS_STREAM_TOPIC_KEY,
                SHORT_LINK_STATS_STREAM_GROUP_KEY,
                id
            );
        } catch (Exception e) {
            log.error("ACK 失败，但业务已成功且已标记，PEL 巡检会补偿: {}", id, e);
        }
    }
}
