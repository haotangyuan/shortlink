package dev.haotangyuan.shortlink.dto.req;

import lombok.Data;

/**
 * 短链接分组排序请求参数
 * @author: haotangyuan
 */
@Data
public class GroupSortReqDTO {

    /**
     * 分组名
     */
    private String gid;

    /**
     * 排序
     */
    private Integer sortOrder;
}
