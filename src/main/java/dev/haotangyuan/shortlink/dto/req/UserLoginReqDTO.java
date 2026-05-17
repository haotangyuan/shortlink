package dev.haotangyuan.shortlink.dto.req;

import lombok.Data;

/**
 * 用户登录请求参数
 * @author: haotangyuan
 */
@Data
public class UserLoginReqDTO {
    /**
     * 用户名
     */
    private String username;

    /**
     * 密码
     */
    private String password;
}
