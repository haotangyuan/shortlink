package dev.haotangyuan.shortlink.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 短链接批量创建响应对象
 * @author: haotangyuan
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LinkBatchCreateVO {

    /**
     * 成功数量
     */
    private Integer total;

    /**
     * 批量创建返回参数
     */
    private List<LinkBaseInfoVO> baseLinkInfos;
}
