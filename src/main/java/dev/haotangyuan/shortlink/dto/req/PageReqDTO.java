package dev.haotangyuan.shortlink.dto.req;

import lombok.Data;

/**
 * 分页请求基类
 * @author: haotangyuan
 */
@Data
public class PageReqDTO {
    private long current = 1;
    private long size = 10;
}
