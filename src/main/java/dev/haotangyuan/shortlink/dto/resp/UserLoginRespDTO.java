package dev.haotangyuan.shortlink.dto.resp;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 用户登录接口返回响应
 * @author: haotangyuan
 */
@Data
@AllArgsConstructor
public class UserLoginRespDTO {
    /**
     * 用户 token
     */
    private String token;
}
