package dev.haotangyuan.shortlink.dto.req;

import lombok.Data;

/**
 * 新增短链接分组请求参数
 *
 * @author: haotangyuan
 */
@Data
public class GroupSaveReqDTO {

    /**
     * 分组名称
     */
    private String name;
}
