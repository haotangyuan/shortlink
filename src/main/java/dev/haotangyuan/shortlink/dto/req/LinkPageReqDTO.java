package dev.haotangyuan.shortlink.dto.req;

import lombok.Data;

/**
 * 短链接分页请求参数
 * @author: haotangyuan
 */
@Data
public class LinkPageReqDTO extends PageReqDTO {

    /**
     * 分组标识
     */
    private String gid;

    /**
     * 排序标识
     */
    private String orderTag;
}
