package dev.haotangyuan.shortlink.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import dev.haotangyuan.shortlink.common.biz.user.UserContext;
import dev.haotangyuan.shortlink.common.convention.exception.ClientException;
import dev.haotangyuan.shortlink.dao.entity.AiMessageDO;
import dev.haotangyuan.shortlink.dao.entity.AiSessionDO;
import dev.haotangyuan.shortlink.dao.mapper.AiMessageMapper;
import dev.haotangyuan.shortlink.dao.mapper.AiSessionMapper;
import dev.haotangyuan.shortlink.service.AiSessionService;
import dev.haotangyuan.shortlink.vo.AiMessageVO;
import dev.haotangyuan.shortlink.vo.AiSessionVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

/**
 * AI 会话管理服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiSessionServiceImpl extends ServiceImpl<AiSessionMapper, AiSessionDO>
        implements AiSessionService {

    private final AiMessageMapper aiMessageMapper;

    @Override
    public void getOrCreateSession(String sessionId) {
        String username = Objects.requireNonNull(UserContext.getUsername(), "用户未登录");
        // 查询是否已存在
        LambdaQueryWrapper<AiSessionDO> qw = Wrappers.lambdaQuery(AiSessionDO.class)
                .eq(AiSessionDO::getSessionId, sessionId)
                .eq(AiSessionDO::getDelFlag, 0);
        AiSessionDO existingSession = baseMapper.selectOne(qw);
        if (existingSession != null) {
            if (!Objects.equals(existingSession.getUsername(), username)) {
                throw new ClientException("会话不存在");
            }
            return;
        }
        // 不存在则创建
        AiSessionDO session = AiSessionDO.builder()
                .sessionId(sessionId)
                .username(username)
                .title("新对话")
                .build();
        baseMapper.insert(session);
    }

    @Override
    public List<AiSessionVO> listSessions() {
        String username = Objects.requireNonNull(UserContext.getUsername(), "用户未登录");
        LambdaQueryWrapper<AiSessionDO> qw = Wrappers.lambdaQuery(AiSessionDO.class)
                .eq(AiSessionDO::getUsername, username)
                .eq(AiSessionDO::getDelFlag, 0)
                .orderByDesc(AiSessionDO::getUpdateTime);
        return baseMapper.selectList(qw).stream()
                .map(s -> AiSessionVO.builder()
                        .sessionId(s.getSessionId())
                        .title(s.getTitle())
                        .createTime(s.getCreateTime())
                        .updateTime(s.getUpdateTime())
                        .build())
                .toList();
    }

    @Override
    public List<AiMessageVO> getSessionMessages(String sessionId) {
        verifySessionOwnership(sessionId);
        LambdaQueryWrapper<AiMessageDO> qw = Wrappers.lambdaQuery(AiMessageDO.class)
                .eq(AiMessageDO::getSessionId, sessionId)
                .eq(AiMessageDO::getDelFlag, 0)
                .orderByAsc(AiMessageDO::getCreateTime)
                .orderByAsc(AiMessageDO::getId);
        return aiMessageMapper.selectList(qw).stream()
                .map(m -> AiMessageVO.builder()
                        .role(m.getRole())
                        .content(m.getContent())
                        .createTime(m.getCreateTime())
                        .build())
                .toList();
    }

    @Override
    public void saveMessage(String sessionId, String role, String content) {
        AiMessageDO message = AiMessageDO.builder()
                .sessionId(sessionId)
                .role(role)
                .content(content)
                .build();
        aiMessageMapper.insert(message);
        // 更新会话的 update_time
        LambdaUpdateWrapper<AiSessionDO> uw = Wrappers.lambdaUpdate(AiSessionDO.class)
                .eq(AiSessionDO::getSessionId, sessionId)
                .setSql("update_time = NOW()");
        baseMapper.update(null, uw);
    }

    @Override
    public void updateSessionTitle(String sessionId, String title) {
        LambdaUpdateWrapper<AiSessionDO> uw = Wrappers.lambdaUpdate(AiSessionDO.class)
                .eq(AiSessionDO::getSessionId, sessionId)
                .eq(AiSessionDO::getDelFlag, 0)
                .set(AiSessionDO::getTitle, title);
        baseMapper.update(null, uw);
    }

    @Override
    public void deleteSession(String sessionId) {
        verifySessionOwnership(sessionId);
        // 逻辑删除会话
        LambdaUpdateWrapper<AiSessionDO> suw = Wrappers.lambdaUpdate(AiSessionDO.class)
                .eq(AiSessionDO::getSessionId, sessionId)
                .set(AiSessionDO::getDelFlag, 1);
        baseMapper.update(null, suw);
        // 逻辑删除消息
        LambdaUpdateWrapper<AiMessageDO> muw = Wrappers.lambdaUpdate(AiMessageDO.class)
                .eq(AiMessageDO::getSessionId, sessionId)
                .set(AiMessageDO::getDelFlag, 1);
        aiMessageMapper.update(null, muw);
    }

    @Override
    public long getMessageCount(String sessionId) {
        LambdaQueryWrapper<AiMessageDO> qw = Wrappers.lambdaQuery(AiMessageDO.class)
                .eq(AiMessageDO::getSessionId, sessionId)
                .eq(AiMessageDO::getDelFlag, 0);
        return aiMessageMapper.selectCount(qw);
    }

    private void verifySessionOwnership(String sessionId) {
        String username = Objects.requireNonNull(UserContext.getUsername(), "用户未登录");
        LambdaQueryWrapper<AiSessionDO> qw = Wrappers.lambdaQuery(AiSessionDO.class)
                .eq(AiSessionDO::getSessionId, sessionId)
                .eq(AiSessionDO::getUsername, username)
                .eq(AiSessionDO::getDelFlag, 0);
        if (baseMapper.selectCount(qw) == 0) {
            throw new ClientException("会话不存在");
        }
    }
}
