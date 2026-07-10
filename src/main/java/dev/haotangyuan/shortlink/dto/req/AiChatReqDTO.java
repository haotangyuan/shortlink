package dev.haotangyuan.shortlink.dto.req;

import lombok.Data;

/**
 * AI 对话请求参数
 */
@Data
public class AiChatReqDTO {

    private String message;

    private String sessionId;
}
