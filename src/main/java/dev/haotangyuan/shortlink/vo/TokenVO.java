package dev.haotangyuan.shortlink.vo;

import lombok.Builder;
import lombok.Data;

import java.util.Date;

/**
 * API 访问令牌返回对象（列表）
 */
@Data
@Builder
public class TokenVO {

    /**
     * token 脱敏
     */
    private String tokenMasked;

    /**
     * 令牌名称
     */
    private String name;

    /**
     * 启用标识 0：启用 1：未启用
     */
    private Integer enableStatus;

    /**
     * 有效期（时间戳，单位毫秒），null 表示永不过期
     */
    private Date validDate;

    /**
     * 描述
     */
    private String describe;
}

