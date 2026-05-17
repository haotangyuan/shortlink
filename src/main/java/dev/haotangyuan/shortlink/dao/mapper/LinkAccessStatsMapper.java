package dev.haotangyuan.shortlink.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import dev.haotangyuan.shortlink.dao.entity.LinkAccessStatsDO;
import dev.haotangyuan.shortlink.dto.req.GroupStatsReqDTO;
import dev.haotangyuan.shortlink.dto.req.LinkStatsReqDTO;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Date;
import java.util.List;

/**
 * 短链接基础访问统计持久层
 * @author: haotangyuan
 */
public interface LinkAccessStatsMapper extends BaseMapper<LinkAccessStatsDO> {

    /**
     * 记录基础访问监控数据
     * @param linkAccessStatsDO 访问统计实体
     */
    @Insert("""
            INSERT INTO t_link_access_stats (
                full_short_url, date, pv, uv, uip, hour, weekday, create_time, update_time, del_flag
            )
            VALUES (
                #{linkAccessStats.fullShortUrl},
                #{linkAccessStats.date},
                #{linkAccessStats.pv},
                #{linkAccessStats.uv},
                #{linkAccessStats.uip},
                #{linkAccessStats.hour},
                #{linkAccessStats.weekday},
                NOW(), NOW(), 0
            )
            ON DUPLICATE KEY UPDATE
                pv = pv + #{linkAccessStats.pv},
                uv = uv + #{linkAccessStats.uv},
                uip = uip + #{linkAccessStats.uip}
            """)
    void shortLinkAccessStats(@Param("linkAccessStats") LinkAccessStatsDO linkAccessStatsDO);

    /**
     * 根据短链接获取指定日期内基础监控数据
     * @param linkStatsReqDTO 查询参数
     * @return 基础访问统计列表
     */
    @Select("""
            SELECT
                tlas.date,
                SUM(tlas.pv)  AS pv,
                SUM(tlas.uv)  AS uv,
                SUM(tlas.uip) AS uip
            FROM t_link tl
            INNER JOIN t_link_access_stats tlas
                ON tl.full_short_url = tlas.full_short_url
            WHERE tlas.full_short_url = #{param.fullShortUrl}
              AND tl.gid = #{param.gid}
              AND tl.del_flag = '0'
              AND tl.enable_status = #{param.enableStatus}
              AND tlas.date BETWEEN #{param.startDate} AND #{param.endDate}
            GROUP BY tlas.full_short_url, tl.gid, tlas.date
            """)
    List<LinkAccessStatsDO> listStatsByShortLink(@Param("param") LinkStatsReqDTO linkStatsReqDTO);

    /**
     * 根据短链接获取指定日期内小时基础监控数据
     * @param linkStatsReqDTO 查询参数
     * @return 基础访问统计列表
     */
    @Select("""
            SELECT
                tlas.hour,
                SUM(tlas.pv) AS pv
            FROM t_link tl
            INNER JOIN t_link_access_stats tlas
                ON tl.full_short_url = tlas.full_short_url
            WHERE tlas.full_short_url = #{param.fullShortUrl}
              AND tl.gid = #{param.gid}
              AND tl.del_flag = '0'
              AND tl.enable_status = #{param.enableStatus}
              AND tlas.date BETWEEN #{param.startDate} AND #{param.endDate}
            GROUP BY tlas.full_short_url, tl.gid, tlas.hour
            """)
    List<LinkAccessStatsDO> listHourStatsByShortLink(@Param("param") LinkStatsReqDTO linkStatsReqDTO);

    /**
     * 根据短链接获取指定日期内小时基础监控数据
     * @param linkStatsReqDTO 查询参数
     * @return 基础访问统计列表
     */
    @Select("""
            SELECT
                tlas.weekday,
                SUM(tlas.pv) AS pv
            FROM t_link tl
            INNER JOIN t_link_access_stats tlas
                ON tl.full_short_url = tlas.full_short_url
            WHERE tlas.full_short_url = #{param.fullShortUrl}
              AND tl.gid = #{param.gid}
              AND tl.del_flag = '0'
              AND tl.enable_status = #{param.enableStatus}
              AND tlas.date BETWEEN #{param.startDate} AND #{param.endDate}
            GROUP BY tlas.full_short_url, tl.gid, tlas.weekday
            """)
    List<LinkAccessStatsDO> listWeekdayStatsByShortLink(@Param("param") LinkStatsReqDTO linkStatsReqDTO);

    /**
     * 根据分组获取指定日期内基础监控数据
     * @param groupStatsReqDTO 查询参数
     * @return 基础访问统计列表
     */
    @Select("""
            SELECT
                tlas.date,
                SUM(tlas.pv)  AS pv,
                SUM(tlas.uv)  AS uv,
                SUM(tlas.uip) AS uip
            FROM t_link tl
            INNER JOIN t_link_access_stats tlas
                    ON tl.full_short_url = tlas.full_short_url
            WHERE tl.gid = #{param.gid}
              AND tl.del_flag = '0'
              AND tl.enable_status = '0'
              AND tlas.date BETWEEN #{param.startDate} AND #{param.endDate}
            GROUP BY tl.gid, tlas.date
            """)
    List<LinkAccessStatsDO> listStatsByGroup(@Param("param") GroupStatsReqDTO groupStatsReqDTO);

    /**
     * 根据分组获取指定日期内小时基础监控数据
     * @param groupStatsReqDTO 查询参数
     * @return 基础访问统计列表
     */
    @Select("""
            SELECT
                tls.hour,
                SUM(tls.pv) AS pv
            FROM
                t_link tl
                INNER JOIN t_link_access_stats tls
                    ON tl.full_short_url = tls.full_short_url
            WHERE
                tl.gid = #{param.gid}
                AND tl.del_flag = '0'
                AND tl.enable_status = '0'
                AND tls.date BETWEEN #{param.startDate} AND #{param.endDate}
            GROUP BY
                tl.gid,
                tls.hour
            """)
    List<LinkAccessStatsDO> listHourStatsByGroup(@Param("param") GroupStatsReqDTO groupStatsReqDTO);

    /**
     * 根据分组获取指定日期内星期基础监控数据
     * @param groupStatsReqDTO 查询参数
     * @return 基础访问统计列表
     */
    @Select("""
            SELECT
                tlas.weekday,
                SUM(tlas.pv) AS pv
            FROM t_link tl
            INNER JOIN t_link_access_stats tlas
                ON tl.full_short_url = tlas.full_short_url
            WHERE tl.gid = #{param.gid}
              AND tl.del_flag = '0'
              AND tl.enable_status = '0'
              AND tlas.date BETWEEN #{param.startDate} AND #{param.endDate}
            GROUP BY tl.gid, tlas.weekday
            """)
    List<LinkAccessStatsDO> listWeekdayStatsByGroup(@Param("param") GroupStatsReqDTO groupStatsReqDTO);

    /**
     * 查询单个短链接今日 PV 总数（从 stats 表）
     * @param fullShortUrl 完整短链接
     * @param today 今日日期
     * @return 今日 PV 总数
     */
    @Select("""
            SELECT
                COALESCE(SUM(tlas.pv), 0)
            FROM t_link_access_stats tlas
            WHERE tlas.full_short_url = #{fullShortUrl}
              AND tlas.del_flag = '0'
              AND DATE(tlas.date) = DATE(#{today})
              AND EXISTS (
                  SELECT 1
                  FROM t_link tl
                  WHERE tl.full_short_url = tlas.full_short_url
                    AND tl.del_flag = '0'
                    AND tl.enable_status = '0'
              )
            GROUP BY tlas.full_short_url
            """)
    int sumTodayPvByShortUrl(@Param("fullShortUrl") String fullShortUrl,
                             @Param("today") Date today);

}
