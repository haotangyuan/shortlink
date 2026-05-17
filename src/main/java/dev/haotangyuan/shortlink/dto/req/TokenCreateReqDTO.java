package dev.haotangyuan.shortlink.dto.req;

import lombok.Data;

import java.util.Date;

/**
 * 创建 API 访问令牌请求
 */
@Data
public class TokenCreateReqDTO {

    /**
     * 令牌名称
     */
    private String name;

    /**
     * 描述
     */
    private String describe;

    /**
     * 有效期，null 表示永不过期
     */
    private Date validDate;
}

