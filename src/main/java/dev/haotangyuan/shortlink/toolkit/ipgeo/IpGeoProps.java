package dev.haotangyuan.shortlink.toolkit.ipgeo;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * IP 地理位置查询配置
 *
 * @author: haotangyuan
 */
@Data
@ConfigurationProperties(prefix = "short-link.stats.locale")
public class IpGeoProps {

    /**
     * IP 地理位置查询服务提供商，支持 local（本地库）和 amap（高德地图）
     */
    private String provider = "local";

    /**
     * 本地库配置
     */
    private Local local = new Local();

    /**
     * 高德地图配置
     */
    private Amap amap = new Amap();

    @Data
    public static class Local {
        private String dbPath;
    }

    @Data
    public static class Amap {
        private String key;
        private String endpoint;
        private int timeout;
    }
}
