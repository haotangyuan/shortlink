package dev.haotangyuan.shortlink.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 用户登录接口返回响应
 * @author: haotangyuan
 */
@Data
@AllArgsConstructor
public class UserLoginVO {
    /**
     * 用户 token
     */
    private String token;
}
