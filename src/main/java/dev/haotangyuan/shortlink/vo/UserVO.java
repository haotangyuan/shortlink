package dev.haotangyuan.shortlink.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import dev.haotangyuan.shortlink.common.serialize.PhoneDesensitizationSerializer;
import lombok.Data;

/**
 * 用户返回参数响应
 *
 * @author: haotangyuan
 */
@Data
public class UserVO {
    /**
     * ID
     */
    private Long id;

    /**
     * 用户名
     */
    private String username;

    /**
     * 真实姓名
     */
    private String realName;

    /**
     * 手机号
     */
    @JsonSerialize(using = PhoneDesensitizationSerializer.class)
    private String phone;

    /**
     * 邮箱
     */
    private String mail;
}
