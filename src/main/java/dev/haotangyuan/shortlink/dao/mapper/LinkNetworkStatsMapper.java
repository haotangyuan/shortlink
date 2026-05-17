package dev.haotangyuan.shortlink.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import dev.haotangyuan.shortlink.dao.entity.LinkNetworkStatsDO;
import dev.haotangyuan.shortlink.dto.req.GroupStatsReqDTO;
import dev.haotangyuan.shortlink.dto.req.LinkStatsReqDTO;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 访问网络统计持久层
 * @author: haotangyuan
 */
public interface LinkNetworkStatsMapper extends BaseMapper<LinkNetworkStatsDO> {

    /**
     * 记录访问设备监控数据
     * @param linkNetworkStatsDO 访问网络统计实体
     */
    @Insert("""
            INSERT INTO t_link_network_stats (
                full_short_url, date, cnt, network, create_time, update_time, del_flag
            )
            VALUES (
                #{linkNetworkStats.fullShortUrl},
                #{linkNetworkStats.date},
                #{linkNetworkStats.cnt},
                #{linkNetworkStats.network},
                NOW(), NOW(), 0
            )
            ON DUPLICATE KEY UPDATE
                cnt = cnt + #{linkNetworkStats.cnt}
            """)
    void shortLinkNetworkStats(@Param("linkNetworkStats") LinkNetworkStatsDO linkNetworkStatsDO);

    /**
     * 根据短链接获取指定日期内访问网络监控数据
     * @param linkStatsReqDTO 查询参数
     * @return 访问网络统计列表
     */
    @Select("""
            SELECT
                tlns.network,
                SUM(tlns.cnt) AS cnt
            FROM t_link tl
            INNER JOIN t_link_network_stats tlns
                ON tl.full_short_url = tlns.full_short_url
            WHERE tlns.full_short_url = #{param.fullShortUrl}
              AND tl.gid = #{param.gid}
              AND tl.del_flag = '0'
              AND tl.enable_status = #{param.enableStatus}
              AND tlns.date BETWEEN #{param.startDate} AND #{param.endDate}
            GROUP BY tlns.full_short_url, tl.gid, tlns.network
            """)
    List<LinkNetworkStatsDO> listNetworkStatsByShortLink(@Param("param") LinkStatsReqDTO linkStatsReqDTO);

    /**
     * 根据分组获取指定日期内访问网络监控数据
     * @param groupStatsReqDTO 查询参数
     * @return 访问网络统计列表
     */
    @Select("""
            SELECT
                tlns.network,
                SUM(tlns.cnt) AS cnt
            FROM t_link tl
            INNER JOIN t_link_network_stats tlns
                ON tl.full_short_url = tlns.full_short_url
            WHERE tl.gid = #{param.gid}
              AND tl.del_flag = '0'
              AND tl.enable_status = '0'
              AND tlns.date BETWEEN #{param.startDate} AND #{param.endDate}
            GROUP BY tl.gid, tlns.network
            """)
    List<LinkNetworkStatsDO> listNetworkStatsByGroup(@Param("param") GroupStatsReqDTO groupStatsReqDTO);
}
