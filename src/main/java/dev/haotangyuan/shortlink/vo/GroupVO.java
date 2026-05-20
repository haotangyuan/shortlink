package dev.haotangyuan.shortlink.vo;

import lombok.Data;

/**
 * 短链接分组返回实体
 * @author: haotangyuan
 */
@Data
public class GroupVO {
    /**
     * 分组标识
     */
    private String gid;

    /**
     * 分组名称
     */
    private String name;

    /**
     * 分组排序
     */
    private Integer sortOrder;

    /**
     * 分组下短链接数量
     */
    private Integer linkCount;
}
