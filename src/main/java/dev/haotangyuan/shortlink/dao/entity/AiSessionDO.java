package dev.haotangyuan.shortlink.dao.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import dev.haotangyuan.shortlink.common.database.BaseDO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * AI 对话会话持久层实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("t_ai_session")
public class AiSessionDO extends BaseDO {

    /**
     * id
     */
    private Long id;

    /**
     * UUID 会话标识（前端生成）
     */
    private String sessionId;

    /**
     * 所属用户名
     */
    private String username;

    /**
     * 会话标题（首条用户消息截断 30 字）
     */
    private String title;
}
