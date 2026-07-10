package dev.haotangyuan.shortlink.vo;

import com.fasterxml.jackson.databind.ObjectMapper;
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

    @Test
    void serializesSnowflakeIdWithoutJavaScriptPrecisionLoss() throws Exception {
        TokenVO token = TokenVO.builder()
                .id(2075578198782910466L)
                .build();

        String json = new ObjectMapper().writeValueAsString(token);

        org.assertj.core.api.Assertions.assertThat(json)
                .contains("\"id\":\"2075578198782910466\"");
    }
}
