package dev.haotangyuan.shortlink.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * AI 会话列表返回对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiSessionVO {

    /**
     * 会话 UUID
     */
    private String sessionId;

    /**
     * 会话标题
     */
    private String title;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间（最后一条消息时间）
     */
    private Date updateTime;
}
