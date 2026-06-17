package dev.haotangyuan.shortlink.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * AI 消息返回对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiMessageVO {

    /**
     * 角色: user / assistant
     */
    private String role;

    /**
     * 消息文本内容
     */
    private String content;

    /**
     * 创建时间
     */
    private Date createTime;
}
