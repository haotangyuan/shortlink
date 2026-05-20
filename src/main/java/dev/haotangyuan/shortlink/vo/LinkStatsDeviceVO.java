package dev.haotangyuan.shortlink.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 短链接访问设备监控响应参数
 * @author: haotangyuan
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LinkStatsDeviceVO {

    /**
     * 统计
     */
    private Integer cnt;

    /**
     * 设备类型
     */
    private String device;

    /**
     * 占比
     */
    private Double ratio;
}
