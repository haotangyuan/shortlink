package dev.haotangyuan.shortlink.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import dev.haotangyuan.shortlink.dao.entity.LinkOsStatsDO;
import dev.haotangyuan.shortlink.dto.req.GroupStatsReqDTO;
import dev.haotangyuan.shortlink.dto.req.LinkStatsReqDTO;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.HashMap;
import java.util.List;

/**
 * 操作系统访问统计持久层
 * @author: haotangyuan
 */
public interface LinkOsStatsMapper extends BaseMapper<LinkOsStatsDO> {

    /**
     * 记录地区访问监控数据
     * @param linkOsStatsDO 操作系统统计实体
     */
    @Insert("""
            INSERT INTO t_link_os_stats (
                full_short_url, date, cnt, os, create_time, update_time, del_flag
            )
            VALUES (
                #{linkOsStats.fullShortUrl},
                #{linkOsStats.date},
                #{linkOsStats.cnt},
                #{linkOsStats.os},
                NOW(), NOW(), 0
            )
            ON DUPLICATE KEY UPDATE
                cnt = cnt + #{linkOsStats.cnt}
            """)
    void shortLinkOsStats(@Param("linkOsStats") LinkOsStatsDO linkOsStatsDO);

    /**
     * 根据短链接获取指定日期内操作系统监控数据
     * @param linkStatsReqDTO 查询参数
     * @return 操作系统访问统计列表
     */
    @Select("""
            SELECT
                tlos.os,
                SUM(tlos.cnt) AS count
            FROM t_link tl
            INNER JOIN t_link_os_stats tlos
                ON tl.full_short_url = tlos.full_short_url
            WHERE tlos.full_short_url = #{param.fullShortUrl}
              AND tl.gid = #{param.gid}
              AND tl.del_flag = '0'
              AND tl.enable_status = #{param.enableStatus}
              AND tlos.date BETWEEN #{param.startDate} AND #{param.endDate}
            GROUP BY tlos.full_short_url, tl.gid, tlos.os
            """)
    List<HashMap<String, Object>> listOsStatsByShortLink(@Param("param") LinkStatsReqDTO linkStatsReqDTO);

    /**
     * 根据分组获取指定日期内操作系统监控数据
     * @param groupStatsReqDTO 查询参数
     * @return 操作系统访问统计列表
     */
    @Select("""
            SELECT
                tlos.os,
                SUM(tlos.cnt) AS count
            FROM t_link tl
            INNER JOIN t_link_os_stats tlos
                ON tl.full_short_url = tlos.full_short_url
            WHERE tl.gid = #{param.gid}
              AND tl.del_flag = '0'
              AND tl.enable_status = '0'
              AND tlos.date BETWEEN #{param.startDate} AND #{param.endDate}
            GROUP BY tl.gid, tlos.os
            """)
    List<HashMap<String, Object>> listOsStatsByGroup(@Param("param") GroupStatsReqDTO groupStatsReqDTO);
}
