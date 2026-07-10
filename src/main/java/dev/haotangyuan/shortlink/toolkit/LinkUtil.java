package dev.haotangyuan.shortlink.toolkit;

import cn.hutool.core.date.DateUnit;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.URLUtil;
import com.google.common.net.InternetDomainName;
import dev.haotangyuan.shortlink.common.config.GotoDomainWhiteListConfiguration;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.net.IDN;
import java.net.InetAddress;
import java.net.URL;
import java.util.Date;
import java.util.Locale;
import java.util.Optional;

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
        String userAgent = request.getHeader("User-Agent");
        if (userAgent == null || userAgent.isBlank()) {
            return "Unknown";
        }
        userAgent = userAgent.toLowerCase(Locale.ROOT);
        if (userAgent.contains("android")) {
            return "Android";
        } else if (userAgent.contains("iphone") || userAgent.contains("ipad") || userAgent.contains("ipod")) {
            return "iOS";
        } else if (userAgent.contains("windows")) {
            return "Windows";
        } else if (userAgent.contains("mac")) {
            return "Mac";
        } else if (userAgent.contains("x11") || userAgent.contains("linux")) {
            return "Unix";
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
        String userAgent = request.getHeader("User-Agent");
        if (userAgent == null || userAgent.isBlank()) {
            return "Unknown";
        }
        userAgent = userAgent.toLowerCase(Locale.ROOT);
        if (userAgent.contains("edg")) {
            return "Edge";
        } else if (userAgent.contains("msie") || userAgent.contains("trident")) {
            return "Internet Explorer";
        } else if (userAgent.contains("opera") || userAgent.contains("opr")) {
            return "Opera";
        } else if (userAgent.contains("chrome")) {
            return "Chrome";
        } else if (userAgent.contains("safari") && !userAgent.contains("chrome")) {
            return "Safari";
        } else if (userAgent.contains("firefox")) {
            return "Firefox";
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
        String userAgent = request.getHeader("User-Agent");
        if (userAgent == null || userAgent.isBlank()) {
            return "Unknown";
        }
        userAgent = userAgent.toLowerCase(Locale.ROOT);
        if (userAgent.contains("mobile")) {
            return "Mobile";
        } else {
            return "Desktop";
        }
    }

    public static String extractDomain(String url) {
        if (StrUtil.isBlank(url)) return null;
        String u = url.trim();
        if (!u.matches("^[a-zA-Z][a-zA-Z0-9+.-]*://.*$")) u = "http://" + u; // 补scheme

        URL parsed;
        try {
            parsed = URLUtil.url(u);
        } catch (Exception ex) {
            return null;
        }
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
     * 判断 URL 是否为公网域名的 HTTP(S) 链接。
     */
    public static boolean isPublicHttpUrl(String url) {
        if (StrUtil.isBlank(url)) return false;
        String normalized = url.trim();
        if (!normalized.matches("^[a-zA-Z][a-zA-Z0-9+.-]*://.*$")) {
            normalized = "http://" + normalized;
        }
        try {
            URL parsed = URLUtil.url(normalized);
            String protocol = parsed.getProtocol();
            return ("http".equalsIgnoreCase(protocol) || "https".equalsIgnoreCase(protocol))
                    && extractDomain(normalized) != null;
        } catch (Exception ex) {
            return false;
        }
    }

    /**
     * 判断 URL 是否为解析到公网地址的 HTTP(S) 链接。
     */
    public static boolean isSafePublicHttpUrl(String url) {
        if (!isPublicHttpUrl(url)) return false;
        String normalized = url.trim();
        if (!normalized.matches("^[a-zA-Z][a-zA-Z0-9+.-]*://.*$")) {
            normalized = "http://" + normalized;
        }
        try {
            URL parsed = URLUtil.url(normalized);
            InetAddress[] addresses = InetAddress.getAllByName(parsed.getHost());
            if (addresses.length == 0) return false;
            for (InetAddress address : addresses) {
                byte[] bytes = address.getAddress();
                boolean uniqueLocalIpv6 = bytes.length == 16 && (bytes[0] & 0xfe) == 0xfc;
                if (address.isAnyLocalAddress()
                        || address.isLoopbackAddress()
                        || address.isLinkLocalAddress()
                        || address.isSiteLocalAddress()
                        || address.isMulticastAddress()
                        || uniqueLocalIpv6) {
                    return false;
                }
            }
            return true;
        } catch (Exception ex) {
            return false;
        }
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
        URL parsed;
        try {
            parsed = URLUtil.url(u);
        } catch (Exception ex) {
            return null;
        }
        String scheme = parsed.getProtocol();
        String host = parsed.getHost();
        int port = parsed.getPort();
        if (StrUtil.isBlank(host)
                || !("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme))) {
            return null;
        }

        String domain = extractDomain(u);
        if (domain == null) return null;
        // 白名单：使用硬编码映射，避免网络抓取
        if (whiteListCfg != null && Boolean.TRUE.equals(whiteListCfg.getEnable())
                && whiteListCfg.getDetails() != null && domain != null
                && whiteListCfg.getDetails().stream().anyMatch(d -> d.equalsIgnoreCase(domain))) {
            String mapped = WhitelistFavicons.get(domain);
            if (mapped != null) return mapped;
            return "https://" + domain + "/favicon.ico";
        }

        StringBuilder baseUrl = new StringBuilder()
                .append(scheme).append("://").append(host);
        if (port > 0 && port != parsed.getDefaultPort()) {
            baseUrl.append(":").append(port);
        }
        return baseUrl.append("/favicon.ico").toString();
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
