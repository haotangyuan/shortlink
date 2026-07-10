package dev.haotangyuan.shortlink.vo;

import lombok.Data;

/**
 * 分组查询响应参数
 *
 * @author: haotangyuan
 */
@Data
public class GroupLinkCountQueryVO {

    /**
     * 分组标识
     */
    private String gid;

    /**
     * 短链接数
     */
    private Integer linkCount;

    /**
     * 累计访问量
     */
    private Long totalPv;

    /**
     * 今日访问量
     */
    private Long todayPv;
}
