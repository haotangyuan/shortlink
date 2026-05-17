package dev.haotangyuan.shortlink.common.biz.user;

import com.alibaba.ttl.TransmittableThreadLocal;

/**
 * 用户上下文：仅保存 username
 */
public class UserContext {
    private static final ThreadLocal<String> USERNAME_HOLDER = new TransmittableThreadLocal<>();

    /** 设置用户名到上下文 */
    public static void setUsername(String username) {
        USERNAME_HOLDER.set(username);
    }

    /** 兼容旧用法：从对象设置，仅取 username */
    public static void setUser(UserInfoDTO user) {
        if (user != null) {
            setUsername(user.getUsername());
        }
    }

    /** 获取上下文中的用户名 */
    public static String getUsername() {
        return USERNAME_HOLDER.get();
    }

    /** 清理上下文 */
    public static void removeUser() {
        USERNAME_HOLDER.remove();
    }
}
