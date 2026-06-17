package dev.haotangyuan.shortlink.dao.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import dev.haotangyuan.shortlink.common.database.BaseDO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * AI 对话消息持久层实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("t_ai_message")
public class AiMessageDO extends BaseDO {

    /**
     * id
     */
    private Long id;

    /**
     * 所属会话 UUID
     */
    private String sessionId;

    /**
     * 角色: user / assistant
     */
    private String role;

    /**
     * 消息文本内容
     */
    private String content;
}
