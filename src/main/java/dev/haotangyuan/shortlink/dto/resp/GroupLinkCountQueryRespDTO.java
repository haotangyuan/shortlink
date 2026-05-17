package dev.haotangyuan.shortlink.dto.resp;

import lombok.Data;

/**
 * 分组查询响应参数
 * @author: haotangyuan
 */
@Data
public class GroupLinkCountQueryRespDTO {

    /**
     * 分组标识
     */
    private String gid;

    /**
     * 短链接数
     */
    private Integer linkCount;
}
