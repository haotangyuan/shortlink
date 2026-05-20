package dev.haotangyuan.shortlink.toolkit;

import cn.hutool.core.date.DateUnit;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.URLUtil;
import com.google.common.net.InternetDomainName;
import dev.haotangyuan.shortlink.common.config.GotoDomainWhiteListConfiguration;
import dev.haotangyuan.shortlink.toolkit.ipgeo.GeoInfo;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.IDN;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static dev.haotangyuan.shortlink.common.constant.LinkConstant.DEFAULT_CACHE_VALID_TIME;

/**
 * 短链接工具类
 *
 * @author: haotangyuan
 */
@RequiredArgsConstructor
@Component
public class LinkUtil {

    private final GotoDomainWhiteListConfiguration whiteListCfg;

    /**
     * 获取短链接缓存有效时间
     *
     * @param validDate 有效期时间
     * @return 缓存有效时间，单位：ms
     */
    public static long getLinkCacheValidTime(Date validDate) {
        return Optional.ofNullable(validDate)
                .map(each -> DateUtil.between(new Date(), each, DateUnit.MS))
                .orElse(DEFAULT_CACHE_VALID_TIME);
    }

    /**
     * 获取实际访问IP
     *
     * @param request HttpServletResponse对象
     * @return 实际访问IP
     */
    public static String getActualIp(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        String[] headerNames = {
                "X-Forwarded-For",
                "X-Real-IP",
                "Proxy-Client-IP",
                "WL-Proxy-Client-IP",
                "HTTP_CLIENT_IP",
                "HTTP_X_FORWARDED_FOR"
        };
        for (String header : headerNames) {
            String ip = request.getHeader(header);
            if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
                continue;
            }
            String first = ip.split(",")[0].trim();
            if (!first.isEmpty() && !"unknown".equalsIgnoreCase(first)) {
                return first;
            }
        }
        return request.getRemoteAddr();
    }

    /**
     * 获取操作系统
     *
     * @param request HttpServletResponse对象
     * @return 操作系统
     */
    public static String getOs(HttpServletRequest request) {
        String userAgent = request.getHeader("User-Agent").toLowerCase();
        if (userAgent == null) {
            return "Unknown";
        }
        if (userAgent.contains("windows")) {
            return "Windows";
        } else if (userAgent.contains("mac")) {
            return "Mac";
        } else if (userAgent.contains("x11") || userAgent.contains("linux")) {
            return "Unix";
        } else if (userAgent.contains("android")) {
            return "Android";
        } else if (userAgent.contains("iphone") || userAgent.contains("ipad")) {
            return "iOS";
        } else {
            return "Unknown";
        }
    }

    /**
     * 获取浏览器
     *
     * @param request HttpServletResponse对象
     * @return 浏览器
     */
    public static String getBrowser(HttpServletRequest request) {
        String userAgent = request.getHeader("User-Agent").toLowerCase();
        if (userAgent == null) {
            return "Unknown";
        }
        if (userAgent.contains("edg")) {
            return "Edge";
        } else if (userAgent.contains("msie") || userAgent.contains("trident")) {
            return "Internet Explorer";
        } else if (userAgent.contains("chrome")) {
            return "Chrome";
        } else if (userAgent.contains("safari") && !userAgent.contains("chrome")) {
            return "Safari";
        } else if (userAgent.contains("firefox")) {
            return "Firefox";
        } else if (userAgent.contains("opera") || userAgent.contains("opr")) {
            return "Opera";
        } else {
            return "Unknown";
        }
    }

    /**
     * 获取设备类型
     *
     * @param request HttpServletResponse对象
     * @return 设备类型
     */
    public static String getDevice(HttpServletRequest request) {
        String userAgent = request.getHeader("User-Agent").toLowerCase();
        if (userAgent == null) {
            return "Unknown";
        }
        if (userAgent.contains("mobile")) {
            return "Mobile";
        } else {
            return "Desktop";
        }
    }

    /**
     * 获取网络类型
     *
     * @param geoInfo GeoInfo 对象
     * @return 网络类型
     */
    public static String getNetwork(GeoInfo geoInfo) {
        if (geoInfo == null) {
            return "Unknown";
        }
        String isp = geoInfo.getIsp();
        if (isp == null) {
            return "Unknown";
        }
        return isp;
    }

    public static String extractDomain(String url) {
        if (StrUtil.isBlank(url)) return null;
        String u = url.trim();
        if (!u.matches("^[a-zA-Z][a-zA-Z0-9+.-]*://.*$")) u = "http://" + u; // 补scheme

        URL parsed = URLUtil.url(u);
        String host = parsed.getHost();
        if (StrUtil.isBlank(host)) return null;

        host = host.toLowerCase(Locale.ROOT);
        if (host.endsWith(".")) host = host.substring(0, host.length() - 1);
        try {
            host = IDN.toASCII(host);
        } catch (Exception ignore) {
        }

        // IP / localhost 直接忽略
        if ("localhost".equals(host) || host.matches("^\\d+\\.\\d+\\.\\d+\\.\\d+$") || host.contains(":")) return null;

        try {
            InternetDomainName idn = InternetDomainName.from(host);
            if (idn.isUnderPublicSuffix() || idn.isTopPrivateDomain()) {
                return idn.topPrivateDomain().toString(); // eTLD+1
            }
        } catch (IllegalArgumentException ignore) {
        }
        return null;
    }

    /**
     * 获取站点 favicon
     */
    public String getFavicon(String url) {
        if (StrUtil.isBlank(url)) return null;
        String u = url.trim();
        if (!u.matches("^[a-zA-Z][a-zA-Z0-9+.-]*://.*$")) {
            u = "http://" + u;
        }
        URL parsed = URLUtil.url(u);
        String scheme = parsed.getProtocol();
        String host = parsed.getHost();
        int port = parsed.getPort();
        if (StrUtil.isBlank(host)) return null;

        String domain = extractDomain(u);
        // 白名单：使用硬编码映射，避免网络抓取
        if (whiteListCfg != null && Boolean.TRUE.equals(whiteListCfg.getEnable())
                && whiteListCfg.getDetails() != null && domain != null
                && whiteListCfg.getDetails().stream().anyMatch(d -> d.equalsIgnoreCase(domain))) {
            String mapped = WhitelistFavicons.get(domain);
            if (mapped != null) return mapped;
            return "https://" + domain + "/favicon.ico";
        }

        // 非白名单：保留原有网络抓取逻辑
        try {
            String baseUrl;
            StringBuilder base = new StringBuilder()
                    .append(scheme).append("://").append(host);
            if (port > 0 && port != parsed.getDefaultPort()) {
                base.append(":").append(port);
            }
            baseUrl = base.toString();

            // 抓取页面 HTML（限 32KB）
            String html = null;
            try {
                HttpURLConnection conn = (HttpURLConnection) new URL(u).openConnection();
                conn.setConnectTimeout(2000);
                conn.setReadTimeout(2000);
                conn.setInstanceFollowRedirects(true);
                conn.setRequestProperty("User-Agent", "Mozilla/5.0");
                int code = conn.getResponseCode();
                if (code >= 200 && code < 300) {
                    StringBuilder sb = new StringBuilder();
                    try (InputStream in = conn.getInputStream();
                         InputStreamReader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                        char[] buf = new char[4096];
                        int len;
                        int total = 0;
                        while ((len = reader.read(buf)) != -1 && total < 32768) {
                            sb.append(buf, 0, len);
                            total += len;
                        }
                    }
                    html = sb.toString();
                }
            } catch (Exception ignore) {
            }

            // 解析 <link rel="icon" ...>
            if (StrUtil.isNotBlank(html)) {
                Pattern p = Pattern.compile("(?i)<link[^>]+rel=[\"'](?:shortcut\\s+)?icon[\"'][^>]*href=[\"']([^\"']+)[\"']");
                Matcher m = p.matcher(html);
                if (m.find()) {
                    String iconHref = m.group(1).trim();
                    if (iconHref.startsWith("http://") || iconHref.startsWith("https://")) {
                        return iconHref;
                    } else if (iconHref.startsWith("//")) {
                        return scheme + ":" + iconHref;
                    } else if (iconHref.startsWith("/")) {
                        return baseUrl + iconHref;
                    } else {
                        return baseUrl + "/" + iconHref;
                    }
                }
            }

            // 回退 /favicon.ico（尝试 GET 以确认返回类型）
            String fallback = baseUrl + "/favicon.ico";
            try {
                HttpURLConnection c = (HttpURLConnection) new URL(fallback).openConnection();
                c.setRequestMethod("GET");
                c.setConnectTimeout(1500);
                c.setReadTimeout(1500);
                c.setRequestProperty("User-Agent", "Mozilla/5.0");
                int code = c.getResponseCode();
                String ct = c.getContentType();
                if (code >= 200 && code < 300 && ct != null && ct.toLowerCase().startsWith("image")) {
                    return fallback;
                }
            } catch (Exception ignore) {
            }
        } catch (Exception ignore) {
        }

        return null;
    }

    /**
     * 白名单 favicon 硬编码映射
     */
    private static class WhitelistFavicons {
        private static final java.util.Map<String, String> MAP = new java.util.HashMap<>();

        static {
            MAP.put("chanler.dev", "https://chanler.dev/favicon.ico");
            MAP.put("zhihu.com", "https://www.zhihu.com/favicon.ico");
            MAP.put("juejin.cn", "https://lf-web-assets.juejin.cn/obj/juejin-web/xitu_juejin_web/static/favicons/favicon-32x32.png");
            MAP.put("cnblogs.com", "https://www.cnblogs.com/favicon.ico");
            MAP.put("bilibili.com", "https://www.bilibili.com/favicon.ico");
            MAP.put("github.com", "https://github.com/favicon.ico");
            MAP.put("csdn.net", "https://www.csdn.net/favicon.ico");
            MAP.put("weixin.qq.com", "https://res.wx.qq.com/a/wx_fed/assets/res/NTI4MWU5.ico");
            MAP.put("qq.com", "https://www.qq.com/favicon.ico");
            MAP.put("toutiao.com", "https://www.toutiao.com/favicon.ico");
            MAP.put("weibo.com", "https://weibo.com/favicon.ico");
            MAP.put("douban.com", "https://www.douban.com/favicon.ico");
            MAP.put("jianshu.com", "https://www.jianshu.com/favicon.ico");
        }

        static String get(String domain) {
            return MAP.get(domain.toLowerCase(Locale.ROOT));
        }
    }
}
