package dev.haotangyuan.shortlink.dao.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import dev.haotangyuan.shortlink.common.database.BaseDO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * API 访问令牌持久层实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("t_api_token")
public class TokenDO extends BaseDO {

    /**
     *  id
     */
    private Long id;

    /**
     *  用户名
     */
    private String username;

    /**
     * token 哈希（SHA-256 十六进制）
     */
    private String tokenHash;

    /**
     * token 后四位（用于识别显示）
     */
    private String tokenLast4;

    /**
     * 令牌名称
     */
    private String name;

    /**
     * 启用标识 0：启用 1：未启用
     */
    private Integer enableStatus;

    /**
     * 有效期（时间戳），null 表示永不过期
     */
    private Date validDate;

    /**
     * 描述
     */
    @TableField("`describe`")
    private String describe;
}
