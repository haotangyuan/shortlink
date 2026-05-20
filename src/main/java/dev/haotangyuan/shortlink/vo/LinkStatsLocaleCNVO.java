package dev.haotangyuan.shortlink.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 短链接地区监控响应参数
 * @author: haotangyuan
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LinkStatsLocaleCNVO {

    /**
     * 统计
     */
    private Integer cnt;

    /**
     * 地区
     */
    private String locale;

    /**
     * 占比
     */
    private Double ratio;
}
