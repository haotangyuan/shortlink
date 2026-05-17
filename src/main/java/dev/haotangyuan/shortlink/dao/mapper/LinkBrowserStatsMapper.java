package dev.haotangyuan.shortlink.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import dev.haotangyuan.shortlink.dao.entity.LinkBrowserStatsDO;
import dev.haotangyuan.shortlink.dto.req.GroupStatsReqDTO;
import dev.haotangyuan.shortlink.dto.req.LinkStatsReqDTO;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.HashMap;
import java.util.List;

/**
 * @author: haotangyuan
 */
public interface LinkBrowserStatsMapper extends BaseMapper<LinkBrowserStatsDO> {

    /**
     * 记录浏览器访问监控数据
     * @param linkBrowserStatsDO 浏览器统计实体
     */
    @Insert("""
            INSERT INTO t_link_browser_stats (
                full_short_url, date, cnt, browser, create_time, update_time, del_flag
            )
            VALUES (
                #{linkBrowserStats.fullShortUrl},
                #{linkBrowserStats.date},
                #{linkBrowserStats.cnt},
                #{linkBrowserStats.browser},
                NOW(), NOW(), 0
            )
            ON DUPLICATE KEY UPDATE
                cnt = cnt + #{linkBrowserStats.cnt}
            """)
    void shortLinkBrowserStats(@Param("linkBrowserStats") LinkBrowserStatsDO linkBrowserStatsDO);

    /**
     * 根据短链接获取指定日期内浏览器监控数据
     * @param linkStatsReqDTO 查询参数
     * @return 浏览器访问统计列表
     */
    @Select("""
            SELECT
                tlbs.browser,
                SUM(tlbs.cnt) AS count
            FROM t_link tl
            INNER JOIN t_link_browser_stats tlbs
                ON tl.full_short_url = tlbs.full_short_url
            WHERE tlbs.full_short_url = #{param.fullShortUrl}
              AND tl.gid = #{param.gid}
              AND tl.del_flag = '0'
              AND tl.enable_status = #{param.enableStatus}
              AND tlbs.date BETWEEN #{param.startDate} AND #{param.endDate}
            GROUP BY tlbs.full_short_url, tl.gid, tlbs.browser
            """)
    List<HashMap<String, Object>> listBrowserStatsByShortLink(@Param("param") LinkStatsReqDTO linkStatsReqDTO);

    /**
     * 根据分组获取指定日期内浏览器监控数据
     * @param groupStatsReqDTO 查询参数
     * @return 浏览器访问统计列表
     */
    @Select("""
            SELECT
                tlbs.browser,
                SUM(tlbs.cnt) AS count
            FROM t_link tl
            INNER JOIN t_link_browser_stats tlbs
                ON tl.full_short_url = tlbs.full_short_url
            WHERE tl.gid = #{param.gid}
              AND tl.del_flag = '0'
              AND tl.enable_status = '0'
              AND tlbs.date BETWEEN #{param.startDate} AND #{param.endDate}
            GROUP BY tl.gid, tlbs.browser
            """)
    List<HashMap<String, Object>> listBrowserStatsByGroup(@Param("param") GroupStatsReqDTO groupStatsReqDTO);
}
