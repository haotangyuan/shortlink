package dev.haotangyuan.shortlink.service;

import dev.haotangyuan.shortlink.dto.req.TokenCreateReqDTO;
import dev.haotangyuan.shortlink.vo.TokenVO;

import java.util.List;

/**
 * @author: haotangyuan
 */
public interface TokenService {
    /** 创建 API 访问令牌，返回明文 token（仅创建时可见） */
    String createToken(TokenCreateReqDTO req);

    /** 列出当前用户的所有令牌（脱敏显示 token） */
    List<TokenVO> listTokens();

    /** 吊销（删除）令牌 */
    void deleteToken(Long id);

    /** 启用/禁用令牌 */
    void updateStatus(Long id, Boolean enable);
}
