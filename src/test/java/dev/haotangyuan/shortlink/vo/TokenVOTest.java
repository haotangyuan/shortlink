package dev.haotangyuan.shortlink.vo;

import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TokenVOTest {

    @Test
    void exposesIdAndUpdateTimeForListResponse() {
        Date updateTime = new Date(1710000000000L);

        TokenVO token = TokenVO.builder()
                .id(123L)
                .updateTime(updateTime)
                .build();

        assertEquals(123L, token.getId());
        assertEquals(updateTime, token.getUpdateTime());
    }
}
