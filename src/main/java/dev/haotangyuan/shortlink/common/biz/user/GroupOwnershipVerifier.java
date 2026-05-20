package dev.haotangyuan.shortlink.common.biz.user;

import java.util.List;

/**
 * 分组归属校验（Verifier）
 * 仅校验 gid 是否属于当前登录用户（UserContext.username）
 */
public interface GroupOwnershipVerifier {

    /**
     * 断言 gid 属于当前用户
     *
     * @param gid 分组标识
     */
    void assertOwnedByCurrentUser(String gid);

    /**
     * 断言一组 gid 均属于当前用户
     *
     * @param gids 分组标识列表
     */
    void assertAllOwnedByCurrentUser(List<String> gids);
}
