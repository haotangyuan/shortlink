package dev.haotangyuan.shortlink.service.impl;

import dev.haotangyuan.shortlink.common.convention.exception.ClientException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

class UrlTitleServiceImplTest {

    private final UrlTitleServiceImpl service = new UrlTitleServiceImpl();

    @Test
    void rejectsBlankUrl() {
        assertThrows(ClientException.class, () -> service.getTitleByUrl(" "));
    }

    @Test
    void rejectsPrivateNetworkUrl() {
        assertThrows(ClientException.class, () -> service.getTitleByUrl("http://127.0.0.1:8068/actuator/health"));
    }
}
