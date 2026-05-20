package dev.haotangyuan.shortlink.common.constant;

/**
 * Redis Key 常量类
 *
 * @author: haotangyuan
 */
public class RedisKeyConstant {

    /**
     * 用户注册分布式锁
     * 格式：short-link:lock:user-register:{username}
     */
    public static final String LOCK_USER_REGISTER_KEY = "short-link:lock:user-register:";

    /**
     * 用户登录缓存标识
     * 格式：short-link:login:{token}
     */
    public static final String USER_LOGIN_KEY = "short-link:login:";

    /**
     * 分组创建分布式锁
     * 格式：short-link:lock:group-create:{username}
     */
    public static final String LOCK_GROUP_CREATE_KEY = "short-link:lock:group-create:%s";

    /**
     * 短链接跳转前缀 key
     * 格式：short-link:goto:{fullShortUrl}
     */
    public static final String GOTO_SHORT_LINK_KEY = "short-link:goto:%s";

    /**
     * 短链接空值跳转锁前缀 key
     * 格式：short-link:is-null:goto_{fullShortUrl}
     */
    public static final String GOTO_IS_NULL_SHORT_LINK_KEY = "short-link:is-null:goto_%s";

    /**
     * 短链接跳转锁前缀 key
     * 格式：short-link:lock:goto:{fullShortUrl}
     */
    public static final String LOCK_GOTO_SHORT_LINK_KEY = "short-link:lock:goto:%s";

    /**
     * 短链接修改分组 ID 锁前缀 Key
     * 格式：short-link:lock:update-gid:{fullShortUrl}
     */
    public static final String LOCK_GID_UPDATE_KEY = "short-link:lock:update-gid:%s";

    /**
     * 短码号段分配全局 Key
     * 格式：short-link:allocation:global
     */
    public static final String SHORT_CODE_ALLOCATION_KEY = "short-link:allocation:global";

    /**
     * 短链接幂等操作锁前缀 key
     * 格式：short-link:idempotent:{messageId}
     */
    public static final String IDEMPOTENT_KEY_PREFIX = "short-link:idempotent:%s";

    /**
     * 短链接监控消息保存队列 Topic 缓存标识
     */
    public static final String SHORT_LINK_STATS_STREAM_TOPIC_KEY = "short-link:stats-stream";

    /**
     * 短链接监控消息保存队列 Group 缓存标识
     */
    public static final String SHORT_LINK_STATS_STREAM_GROUP_KEY = "short-link:stats-stream:only-group";

    /**
     * GID 反向归属索引（旧方案，废弃）
     * 格式：short-link:gid-owner:{gid} -> username
     */
    public static final String GID_OWNER_KEY = "short-link:gid-owner:%s";

    /**
     * 用户 GID 正向索引集合
     * 格式：short-link:user-gids:{username} (Set of gid)
     */
    public static final String USER_GIDS_KEY = "short-link:user-gids:%s";

    /**
     * Admin / Client 会话 Token 映射
     * 格式：short-link:session:{token} -> username
     */
    public static final String SESSION_KEY = "short-link:session:%s";

    /**
     * Core API 访问令牌映射
     * 明文映射（旧方案，逐步废弃）：short-link:api-token:{token} -> username
     */
    public static final String API_TOKEN_KEY_PREFIX = "short-link:api-token:%s";

    /**
     * Core API 访问令牌映射（哈希值）
     * 格式：short-link:api-token-h:{sha256(token)} -> username
     */
    public static final String API_TOKEN_HASH_KEY = "short-link:api-token-h:%s";

    /**
     * 短链接 UV HyperLogLog 缓存标识
     * 格式：short-link:stats:uv:{v}:{fullShortUrl}
     * v = epochDay(Asia/Shanghai) % 2
     */
    public static final String STATS_UV_HLL_KEY = "short-link:stats:uv:%d:%s";

    /**
     * UV HLL 前缀（批量 PFCOUNT 使用）
     * 格式：short-link:stats:uv:{v}:
     */
    public static final String STATS_UV_PREFIX = "short-link:stats:uv:%d:";

    /**
     * 短链接 UIP HyperLogLog 缓存标识
     * 格式：short-link:stats:uip:{v}:{fullShortUrl}
     * v = epochDay(Asia/Shanghai) % 2
     */
    public static final String STATS_UIP_HLL_KEY = "short-link:stats:uip:%d:%s";

    /**
     * UIP HLL 前缀（批量 PFCOUNT 使用）
     * 格式：short-link:stats:uip:{v}:
     */
    public static final String STATS_UIP_PREFIX = "short-link:stats:uip:%d:";

    /**
     * UV 活跃集合 key（用于清理）
     * 格式：short-link:stats:uv:active:{v}
     * v = epochDay(Asia/Shanghai) % 2
     */
    public static final String STATS_UV_ACTIVE_KEY = "short-link:stats:uv:active:%d";

    /**
     * UIP 活跃集合 key（用于清理）
     * 格式：short-link:stats:uip:active:{v}
     * v = epochDay(Asia/Shanghai) % 2
     */
    public static final String STATS_UIP_ACTIVE_KEY = "short-link:stats:uip:active:%d";
}
