package dev.haotangyuan.shortlink.common.config;

import dev.haotangyuan.shortlink.mq.consumer.LinkStatsSaveConsumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.stream.StreamListener;
import org.springframework.data.redis.stream.StreamMessageListenerContainer;
import org.springframework.data.redis.stream.Subscription;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

import static dev.haotangyuan.shortlink.common.constant.RedisKeyConstant.SHORT_LINK_STATS_STREAM_GROUP_KEY;
import static dev.haotangyuan.shortlink.common.constant.RedisKeyConstant.SHORT_LINK_STATS_STREAM_TOPIC_KEY;

/**
 * Redis Stream 消息队列配置
 *
 * @author: haotangyuan
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class RedisStreamConfiguration {

    private final RedisConnectionFactory redisConnectionFactory;
    private final LinkStatsSaveConsumer linkStatsSaveConsumer;

    // 每 CPU ≈1.5 个消费者，向下取整，至少 1 个
    private final int consumerCount = Math.max(1, (int) Math.floor(Runtime.getRuntime().availableProcessors() * 1.5));
    private static final long THROUGHPUT_LOG_INTERVAL_MS = 300_000L;
    private final LongAdder consumeCounter = new LongAdder();
    private final AtomicLong lastThroughputLogTime = new AtomicLong(System.currentTimeMillis());

    @Bean
    public ExecutorService asyncStreamConsumer() {
        AtomicInteger index = new AtomicInteger();
        log.info("消费者线程池配置，线程数：{}", consumerCount);
        return Executors.newFixedThreadPool(consumerCount, r -> {
            Thread t = new Thread(r, "stream_consumer_stats_" + index.getAndIncrement());
            t.setDaemon(true);
            return t;
        });
    }

    @Bean(initMethod = "start", destroyMethod = "stop")
    public StreamMessageListenerContainer<String, MapRecord<String, String, String>> streamMessageListenerContainer(
            ExecutorService asyncStreamConsumer) {
        StreamMessageListenerContainer.StreamMessageListenerContainerOptions<String, MapRecord<String, String, String>> options =
                StreamMessageListenerContainer.StreamMessageListenerContainerOptions
                        .builder()
                        .batchSize(100) // 每个消费者每次拉取 100 条
                        .executor(asyncStreamConsumer)
                        .pollTimeout(Duration.ofMillis(500))
                        .build();
        return StreamMessageListenerContainer.create(redisConnectionFactory, options);
    }

    /**
     * 创建多个消费者订阅，实现真正的并行消费
     */
    @Bean
    public List<Subscription> shortLinkStatsSaveConsumerSubscriptions(
            StreamMessageListenerContainer<String, MapRecord<String, String, String>> container) {

        List<Subscription> subscriptions = new ArrayList<>();

        StreamListener<String, MapRecord<String, String, String>> loggingListener = message -> {
            consumeCounter.increment();
            linkStatsSaveConsumer.onMessage(message);

            long now = System.currentTimeMillis();
            long last = lastThroughputLogTime.get();
            if (now - last >= THROUGHPUT_LOG_INTERVAL_MS && lastThroughputLogTime.compareAndSet(last, now)) {
                long processed = consumeCounter.sumThenReset();
                if (processed > 0) {
                    long tps = processed * 1000 / THROUGHPUT_LOG_INTERVAL_MS;
                    log.info("Stream 消费吞吐量: {} msgs / {} ms (≈{} TPS)",
                            processed, THROUGHPUT_LOG_INTERVAL_MS, tps);
                }
            }
        };

        for (int i = 0; i < consumerCount; i++) {
            StreamMessageListenerContainer.StreamReadRequest<String> request =
                    StreamMessageListenerContainer.StreamReadRequest.builder(
                                    StreamOffset.create(SHORT_LINK_STATS_STREAM_TOPIC_KEY, ReadOffset.lastConsumed()))
                            .cancelOnError(throwable -> false)
                            .consumer(Consumer.from(SHORT_LINK_STATS_STREAM_GROUP_KEY, "stats-consumer-" + i))
                            .autoAcknowledge(false)
                            .build();

            Subscription subscription = container.register(request, loggingListener);
            subscriptions.add(subscription);
            log.info("注册消费者: stats-consumer-{}", i);
        }

        return subscriptions;
    }
}
