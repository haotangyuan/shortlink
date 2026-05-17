package dev.haotangyuan.shortlink.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import dev.haotangyuan.shortlink.dao.entity.LinkDeviceStatsDO;
import dev.haotangyuan.shortlink.dto.req.GroupStatsReqDTO;
import dev.haotangyuan.shortlink.dto.req.LinkStatsReqDTO;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 访问设备统计持久层
 * @author: haotangyuan
 */
public interface LinkDeviceStatsMapper extends BaseMapper<LinkDeviceStatsDO> {

    /**
     * 记录访问设备监控数据
     * @param linkDeviceStatsDO 访问设备统计实体
     */
    @Insert("""
            INSERT INTO t_link_device_stats (
                full_short_url, date, cnt, device, create_time, update_time, del_flag
            )
            VALUES (
                #{linkDeviceStats.fullShortUrl},
                #{linkDeviceStats.date},
                #{linkDeviceStats.cnt},
                #{linkDeviceStats.device},
                NOW(), NOW(), 0
            )
            ON DUPLICATE KEY UPDATE
                cnt = cnt + #{linkDeviceStats.cnt}
            """)
    void shortLinkDeviceStats(@Param("linkDeviceStats") LinkDeviceStatsDO linkDeviceStatsDO);

    /**
     * 根据短链接获取指定日期内访问设备监控数据
     * @param linkStatsReqDTO 查询参数
     * @return 访问设备统计列表
     */
    @Select("""
            SELECT
                tlds.device,
                SUM(tlds.cnt) AS cnt
            FROM t_link tl
            INNER JOIN t_link_device_stats tlds
                ON tl.full_short_url = tlds.full_short_url
            WHERE tlds.full_short_url = #{param.fullShortUrl}
              AND tl.gid = #{param.gid}
              AND tl.del_flag = '0'
              AND tl.enable_status = #{param.enableStatus}
              AND tlds.date BETWEEN #{param.startDate} AND #{param.endDate}
            GROUP BY tlds.full_short_url, tl.gid, tlds.device
            """)
    List<LinkDeviceStatsDO> listDeviceStatsByShortLink(@Param("param") LinkStatsReqDTO linkStatsReqDTO);

    /**
     * 根据分组获取指定日期内访问设备监控数据
     * @param groupStatsReqDTO 查询参数
     * @return 访问设备统计列表
     */
    @Select("""
            SELECT
                tlds.device,
                SUM(tlds.cnt) AS cnt
            FROM t_link tl
            INNER JOIN t_link_device_stats tlds
                ON tl.full_short_url = tlds.full_short_url
            WHERE tl.gid = #{param.gid}
              AND tl.del_flag = '0'
              AND tl.enable_status = '0'
              AND tlds.date BETWEEN #{param.startDate} AND #{param.endDate}
            GROUP BY tl.gid, tlds.device
            """)
    List<LinkDeviceStatsDO> listDeviceStatsByGroup(@Param("param") GroupStatsReqDTO groupStatsReqDTO);
}
