package dev.haotangyuan.shortlink.dto.req;

import lombok.Data;

/**
 * 短链接访问记录请求参数
 * @author: haotangyuan
 */
@Data
public class LinkStatsAccessRecordReqDTO extends PageReqDTO {

    /**
     * 完整短链接
     */
    private String fullShortUrl;

    /**
     * 分组标识
     */
    private String gid;

    /**
     * 开始日期
     */
    private String startDate;

    /**
     * 结束日期
     */
    private String endDate;

    /**
     * 启用标识 0：启用 1：未启用
     */
    private Integer enableStatus;
}
