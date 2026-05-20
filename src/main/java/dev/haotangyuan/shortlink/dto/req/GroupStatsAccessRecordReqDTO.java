package dev.haotangyuan.shortlink.dto.req;

import lombok.Data;

/**
 * 分组短链接监控访问记录请求参数
 *
 * @author: haotangyuan
 */
@Data
public class GroupStatsAccessRecordReqDTO extends PageReqDTO {

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
}
