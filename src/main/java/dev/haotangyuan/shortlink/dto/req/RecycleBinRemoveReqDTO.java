package dev.haotangyuan.shortlink.dto.req;

import lombok.Data;

/**
 * 回收站移除请求参数
 * @author: haotangyuan
 */
@Data
public class RecycleBinRemoveReqDTO {

    /**
     * 分组标识
     */
    private String gid;

    /**
     * 短链接
     */
    private String fullShortUrl;
}
