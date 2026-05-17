package dev.haotangyuan.shortlink.dto.req;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import dev.haotangyuan.shortlink.dao.entity.LinkDO;
import lombok.Data;

import java.util.List;

/**
 * 分页查询回收站请求参数
 * @author: haotangyuan
 */
@Data
public class RecycleBinLinkPageReqDTO extends Page<LinkDO> {

    /**
     * 分组列表
     */
    private List<String> gidList;
}
