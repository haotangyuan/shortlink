package dev.haotangyuan.shortlink.dto.req;

import lombok.Data;

import java.util.List;

/**
 * 分页查询回收站请求参数
 * @author: haotangyuan
 */
@Data
public class RecycleBinLinkPageReqDTO extends PageReqDTO {

    /**
     * 分组列表
     */
    private List<String> gidList;
}
