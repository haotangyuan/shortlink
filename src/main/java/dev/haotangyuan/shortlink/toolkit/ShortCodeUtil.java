package dev.haotangyuan.shortlink.toolkit;

import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import static dev.haotangyuan.shortlink.common.constant.RedisKeyConstant.SHORT_CODE_ALLOCATION_KEY;

/**
 * 极简短码生成器
 *
 * @author: haotangyuan
 */
@Slf4j
public class ShortCodeUtil {

    /**
     * 字符集
     */
    private static final char[] CHARS = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz".toCharArray();

    /**
     * 参数（仅支持 6/7 位，运行期全用 long）
     */
    private static volatile int LENGTH;           // 6 or 7
    private static volatile long SEGMENT_STEP;    // 号段步长
    private static volatile double PREFETCH_RATIO;// 预取阈值占比 (0,1)
    private static volatile long A;               // 仿射参数 a
    private static volatile long B;               // 仿射参数 b
    private static volatile long N;               // N=62^LENGTH

    /**
     * Redis 依赖
     */
    private static StringRedisTemplate stringRedisTemplate;

    /**
     * 当前段 [start, end] 与游标（包含）
     */
    private static volatile long end = 0; // start > end 表示“无段可用”
    private static final AtomicLong cursor = new AtomicLong(0);

    /**
     * 预取段（极简预取方案）
     */
    private static final AtomicReference<Segment> nextSeg = new AtomicReference<>();
    private static final AtomicBoolean prefetching = new AtomicBoolean(false);

    private static final Object LOCK = new Object();

    private record Segment(long start, long end) {
    }

    private ShortCodeUtil() {
    }

    /*
     * 在应用启动时初始化（使用 ShortCodeProps）。
     * 仅支持 6/7 位；同步预热首个号段，避免首请求落慢路径。
     */
    public static void init(StringRedisTemplate redisTemplate, ShortCodeProps props) {
        Objects.requireNonNull(redisTemplate, "StringRedisTemplate must not be null");
        Objects.requireNonNull(props, "ShortCodeProps must not be null");
        if (stringRedisTemplate != null) {
            throw new IllegalStateException("ShortCodeUtil already initialized");
        }

        LENGTH = (props.getLength() == null ? 6 : props.getLength());
        if (LENGTH != 6 && LENGTH != 7) {
            throw new IllegalArgumentException("shortcode length must be 6 or 7");
        }
        N = pow62(LENGTH);

        SEGMENT_STEP = (props.getSegmentStep() == null ? 100_000L : props.getSegmentStep());
        PREFETCH_RATIO = (props.getPrefetchRatio() == null ? 0.2d : props.getPrefetchRatio());
        if (SEGMENT_STEP <= 0) {
            throw new IllegalArgumentException("segmentStep must be > 0");
        }
        if (!(PREFETCH_RATIO > 0 && PREFETCH_RATIO < 1)) {
            throw new IllegalArgumentException("prefetchRatio must be in (0,1)");
        }

        long aLong = (StrUtil.isBlank(props.getA()))
                ? 1_999_997L
                : Long.parseLong(props.getA().trim());
        if (!isCoprime(aLong, 62)) {
            throw new IllegalArgumentException("affine 'a' must be odd and coprime to 62 and 31");
        }
        long aMax = (Long.MAX_VALUE - (N - 1)) / (N - 1);
        if (aLong <= 0 || aLong > aMax) {
            throw new IllegalArgumentException("affine 'a' too large for LONG safety; max=" + aMax);
        }
        long bLong = (StrUtil.isBlank(props.getB()))
                ? Math.min(19_987_654_321L, N - 1)
                : Long.parseLong(props.getB().trim());
        if (bLong < 0) bLong = (bLong % N + N) % N;
        if (bLong >= N) bLong = bLong % N;
        A = aLong;
        B = bLong;

        stringRedisTemplate = redisTemplate;
        // 启动预热：同步拉首个号段，避免首个请求落慢路径
        Segment first = fetchFromRedis();
        switchTo(first);
    }

    /* 生成下一个短码（固定 LENGTH 位），获取全局序号 i → 仿射置换为 y → Base62 固定长度编码 */
    public static String next() {
        ensureInit();
        long i = nextId();
        long y = mapIndexToY(i);
        return encodeBase62Fixed(y, LENGTH);
    }

    /**
     * 获取下一个全局自增 ID，并预取号段
     */
    private static long nextId() {
        for (; ; ) {
            long c = cursor.getAndIncrement();
            if (c <= end) {
                maybePrefetch();
                return c;
            }

            // 无锁优先切换已预取段
            Segment seg = nextSeg.getAndSet(null);
            if (seg != null) {
                switchTo(seg);
                continue;
            }

            // 慢路径：同步取段并切换（仅一个线程进入）
            synchronized (LOCK) {
                if (cursor.get() <= end) {
                    continue;
                }
                seg = nextSeg.getAndSet(null);
                if (seg == null) {
                    seg = fetchFromRedis();
                }
                switchTo(seg);
            }
        }
    }

    /**
     * 触发异步预取下一个号段，剩余 prefetchRatio 时，预取
     */
    private static void maybePrefetch() {
        // 已有预取结果或预取在则不重复触发
        if (nextSeg.get() != null) return;
        long rem = end - cursor.get() + 1;
        if (rem > (long) (SEGMENT_STEP * PREFETCH_RATIO)) return;
        if (!prefetching.compareAndSet(false, true)) return;
        CompletableFuture.runAsync(() -> {
            try {
                Segment seg = fetchFromRedis();
                nextSeg.compareAndSet(null, seg);
            } catch (Exception ex) {
                log.warn("Prefetch short-code segment failed: {}", ex.getMessage());
            } finally {
                prefetching.set(false);
            }
        });
    }

    /**
     * 切换当前号段为指定段
     */
    private static void switchTo(Segment seg) {
        end = seg.end;
        cursor.set(seg.start);
    }

    /**
     * 从 Redis 获取一个新号段
     */
    private static Segment fetchFromRedis() {
        ensureInit();
        Long val = stringRedisTemplate.opsForValue().increment(SHORT_CODE_ALLOCATION_KEY, SEGMENT_STEP);
        if (val == null) {
            throw new IllegalStateException("Redis INCRBY returned null");
        }
        long newEnd = val - 1;
        long newStart = newEnd - SEGMENT_STEP + 1;
        return new Segment(newStart, newEnd);
    }

    /* Base62 固定长度编码（long 版，定长 char[] 从末位回填） */
    private static String encodeBase62Fixed(long value, int len) {
        char[] out = new char[len];
        long v = value;
        for (int p = len - 1; p >= 0; p--) {
            int idx = (int) (v % 62);
            out[p] = CHARS[idx];
            v /= 62;
        }
        return new String(out);
    }

    /**
     * 确保已初始化
     */
    private static void ensureInit() {
        if (stringRedisTemplate == null || N <= 0 || LENGTH <= 0 || A == 0) {
            throw new IllegalStateException("ShortCodeUtil not initialized. Call ShortCodeUtil.init(...) at startup.");
        }
    }

    /**
     * 判断 a 与 b 是否互素
     */
    private static boolean isCoprime(long a, int b) {
        long x = a, y = b;
        while (y != 0) {
            long t = x % y;
            x = y;
            y = t;
        }
        return x == 1;
    }

    /* 62 的 n 次幂（n 仅为 6 或 7） */
    private static long pow62(int n) {
        long r = 1L;
        for (int i = 0; i < n; i++) r *= 62L;
        return r;
    }

    /* i → y 的仿射映射（long 版） */
    private static long mapIndexToY(long i) {
        long y = (A * i + B) % N;
        if (y < 0) y += N;
        return y;
    }

}
