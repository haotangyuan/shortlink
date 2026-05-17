package dev.haotangyuan.shortlink.dto.req;

import lombok.Data;

/**
 * 回收站保存请求参数
 * @author: haotangyuan
 */
@Data
public class RecycleBinRestoreReqDTO {

    /**
     * 分组标识
     */
    private String gid;

    /**
     * 短链接
     */
    private String fullShortUrl;
}
