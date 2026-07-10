package dev.haotangyuan.shortlink.toolkit;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LinkUtilTest {

    @Test
    void missingUserAgentIsReportedAsUnknown() {
        HttpServletRequest request = mock(HttpServletRequest.class);

        assertEquals("Unknown", LinkUtil.getOs(request));
        assertEquals("Unknown", LinkUtil.getBrowser(request));
        assertEquals("Unknown", LinkUtil.getDevice(request));
    }

    @Test
    void mobileOperatingSystemsAreDetectedBeforeDesktopMarkers() {
        HttpServletRequest androidRequest = requestWithUserAgent(
                "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 Chrome/125.0 Mobile Safari/537.36"
        );
        HttpServletRequest iosRequest = requestWithUserAgent(
                "Mozilla/5.0 (iPhone; CPU iPhone OS 17_5 like Mac OS X) AppleWebKit/605.1.15 Mobile/15E148 Safari/604.1"
        );

        assertEquals("Android", LinkUtil.getOs(androidRequest));
        assertEquals("iOS", LinkUtil.getOs(iosRequest));
    }

    @Test
    void operaIsDetectedBeforeItsChromeCompatibilityMarker() {
        HttpServletRequest request = requestWithUserAgent(
                "Mozilla/5.0 AppleWebKit/537.36 Chrome/125.0 Safari/537.36 OPR/111.0"
        );

        assertEquals("Opera", LinkUtil.getBrowser(request));
    }

    @Test
    void privateAndNonHttpUrlsAreNotSafeForServerSideFetching() {
        assertFalse(LinkUtil.isSafePublicHttpUrl("http://127.0.0.1/internal"));
        assertFalse(LinkUtil.isSafePublicHttpUrl("http://localhost/internal"));
        assertFalse(LinkUtil.isSafePublicHttpUrl("file:///etc/passwd"));
        assertFalse(LinkUtil.isPublicHttpUrl("javascript://github.com/payload"));
    }

    private HttpServletRequest requestWithUserAgent(String userAgent) {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("User-Agent")).thenReturn(userAgent);
        return request;
    }
}
