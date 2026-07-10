package dev.haotangyuan.shortlink.toolkit.ipgeo;

import cn.hutool.http.HttpUtil;
import com.alibaba.fastjson2.JSON;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

/**
 * 高德地图 IP 地理位置查询客户端
 *
 * @author: haotangyuan
 */
public class AmapClient implements IpGeoClient {

    /**
     * 高德地图 Key
     */
    private final String key;

    /**
     * 高德地图 IP 查询接口
     */
    private final String endpoint;

    /**
     * 请求超时时间
     */
    private final int timeout;

    public AmapClient(String key, String endpoint, int timeout) {
        this.key = key;
        this.endpoint = endpoint;
        this.timeout = timeout;
    }

    @Override
    public GeoInfo query(String ip) {
        Map<String, Object> localeParamMap = new HashMap<>();
        localeParamMap.put("key", key);
        localeParamMap.put("ip", ip);
        String localeResultStr = HttpUtil.get(endpoint, localeParamMap, timeout);
        AmapResp r = JSON.parseObject(localeResultStr, AmapResp.class);
        if (r == null || !"1".equals(r.status)) {
            return GeoInfo.builder()
                    .country("Unknown")
                    .province("Unknown")
                    .city("Unknown")
                    .build();
        }
        String province = normalize(r.province);
        String city = normalize(r.city);
        String adcode = normalize(r.adcode);
        String country = (adcode != null && adcode.matches("\\d{6}")) ? "中国" : null;

        return GeoInfo.builder()
                .country(country)
                .province(province)
                .city(city)
                .adcode(adcode)
                .build();
    }

    private static String normalize(String v) {
        if (v == null) return null;
        String s = v.trim();
        return (s.isEmpty() || "[]".equals(s)) ? "Unknown" : s;
    }

    @Data
    private static class AmapResp {
        private String status;
        private String info;
        private String province;
        private String city;
        private String adcode;
    }
}
