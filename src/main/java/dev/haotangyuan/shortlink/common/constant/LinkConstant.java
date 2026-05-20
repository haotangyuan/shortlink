package dev.haotangyuan.shortlink.common.constant;

/**
 * 短链接常量类
 *
 * @author: haotangyuan
 */
public class LinkConstant {

    /**
     * 永久短链接缓存有效时间，单位：ms
     * 默认值为 30 天
     */
    public static final long DEFAULT_CACHE_VALID_TIME = 2626560000L;

    /**
     * UV Cookie 最大有效期（秒）- 3个月
     */
    public static final int UV_COOKIE_MAX_AGE_SECONDS = 60 * 60 * 24 * 90;
}
