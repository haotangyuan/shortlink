package dev.haotangyuan.shortlink.dto.req;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PageReqDTOTest {

    @Test
    void paginationValuesAreKeptWithinSafeBounds() {
        PageReqDTO request = new PageReqDTO();
        request.setCurrent(0);
        request.setSize(10_000);

        assertEquals(1, request.getCurrent());
        assertEquals(100, request.getSize());
    }
}
