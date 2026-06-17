package dev.haotangyuan.shortlink.service;

import dev.haotangyuan.shortlink.vo.AiMessageVO;
import dev.haotangyuan.shortlink.vo.AiSessionVO;

import java.util.List;

/**
 * AI 会话管理服务
 */
public interface AiSessionService {

    /**
     * 幂等创建会话（已存在则跳过）
     */
    void getOrCreateSession(String sessionId);

    /**
     * 获取当前用户的会话列表（按更新时间倒序）
     */
    List<AiSessionVO> listSessions();

    /**
     * 获取指定会话的消息列表（按创建时间正序）
     */
    List<AiMessageVO> getSessionMessages(String sessionId);

    /**
     * 保存一条消息
     */
    void saveMessage(String sessionId, String role, String content);

    /**
     * 更新会话标题
     */
    void updateSessionTitle(String sessionId, String title);

    /**
     * 逻辑删除会话及其所有消息
     */
    void deleteSession(String sessionId);

    /**
     * 获取会话消息数量
     */
    long getMessageCount(String sessionId);
}
