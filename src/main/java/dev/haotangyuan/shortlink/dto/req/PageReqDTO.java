package dev.haotangyuan.shortlink.dto.req;

import lombok.Data;

/**
 * 分页请求基类
 *
 * @author: haotangyuan
 */
@Data
public class PageReqDTO {
    private long current = 1;
    private long size = 10;

    public long getCurrent() {
        return Math.max(1, current);
    }

    public long getSize() {
        return Math.min(100, Math.max(1, size));
    }
}
