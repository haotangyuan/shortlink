package dev.haotangyuan.shortlink.toolkit.ipgeo;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * IP 地理位置查询配置
 * @author: haotangyuan
 */
@Configuration
@EnableConfigurationProperties(IpGeoProps.class)
public class IpGeoConfig {

    @Bean
    @ConditionalOnProperty(prefix = "short-link.stats.locale", name = "provider", havingValue = "local", matchIfMissing = true)
    public IpGeoClient localClient(IpGeoProps p) {
        return new LocalClient(p.getLocal().getDbPath());
    }

    @Bean
    @ConditionalOnProperty(prefix = "short-link.stats.locale", name = "provider", havingValue = "amap", matchIfMissing = true)
    public IpGeoClient AmapClient(IpGeoProps p) {
        return new AmapClient(p.getAmap().getKey(), p.getAmap().getEndpoint(), p.getAmap().getTimeout());
    }
}
