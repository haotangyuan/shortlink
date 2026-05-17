package dev.haotangyuan.shortlink.service.impl;

import dev.haotangyuan.shortlink.service.UrlTitleService;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 简化版 URL 标题获取
 */
@Service
public class UrlTitleServiceImpl implements UrlTitleService {

    private static final Pattern TITLE = Pattern.compile("(?is)<title[^>]*>(.*?)</title>");
    private static final int MAX_BYTES = 8 * 1024;

    @Override
    public String getTitleByUrl(String url) {
        if (url == null || url.isBlank()) {
            return "";
        }
        try {
            HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setConnectTimeout(4000);
            conn.setReadTimeout(4000);
            conn.setRequestProperty("User-Agent", "Mozilla/5.0");
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                StringBuilder sb = new StringBuilder();
                int ch;
                while ((ch = br.read()) != -1 && sb.length() < MAX_BYTES) {
                    sb.append((char) ch);
                }
                Matcher m = TITLE.matcher(sb);
                if (m.find()) {
                    String t = m.group(1).replaceAll("\\s+", " ").trim();
                    return t.length() > 128 ? t.substring(0, 128) : t;
                }
            }
        } catch (Exception ignored) {
        }
        return "";
    }
}