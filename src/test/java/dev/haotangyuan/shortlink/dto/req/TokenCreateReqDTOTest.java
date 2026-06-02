package dev.haotangyuan.shortlink.dto.req;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.text.SimpleDateFormat;
import java.util.TimeZone;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TokenCreateReqDTOTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void parsesConsoleDateTimeFormat() throws Exception {
        TokenCreateReqDTO request = objectMapper.readValue(
                """
                        {
                          "name": "console-token",
                          "validDate": "2026-06-02 23:59:59"
                        }
                        """,
                TokenCreateReqDTO.class
        );

        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        format.setTimeZone(TimeZone.getTimeZone("GMT+8"));
        assertEquals(format.parse("2026-06-02 23:59:59"), request.getValidDate());
    }
}
