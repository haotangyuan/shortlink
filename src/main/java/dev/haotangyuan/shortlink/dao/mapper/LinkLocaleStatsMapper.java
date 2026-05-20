package dev.haotangyuan.shortlink.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import dev.haotangyuan.shortlink.dao.entity.LinkLocaleStatsDO;
import dev.haotangyuan.shortlink.dto.req.GroupStatsReqDTO;
import dev.haotangyuan.shortlink.dto.req.LinkStatsReqDTO;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 地区访问统计持久层
 *
 * @author: haotangyuan
 */
public interface LinkLocaleStatsMapper extends BaseMapper<LinkLocaleStatsDO> {

    /**
     * 短链接地区访问统计
     *
     * @param linkLocaleStatsDO 地区访问统计实体
     */
    @Insert("""
            INSERT INTO t_link_locale_stats (
                full_short_url, date, cnt, country, province, city, adcode, create_time, update_time, del_flag
            )
            VALUES (
                #{linkLocaleStats.fullShortUrl},
                #{linkLocaleStats.date},
                #{linkLocaleStats.cnt},
                #{linkLocaleStats.country},
                #{linkLocaleStats.province},
                #{linkLocaleStats.city},
                #{linkLocaleStats.adcode},
                NOW(), NOW(), 0
            )
            ON DUPLICATE KEY UPDATE
                cnt = cnt + #{linkLocaleStats.cnt}
            """)
    void shortLinkLocaleStats(@Param("linkLocaleStats") LinkLocaleStatsDO linkLocaleStatsDO);

    /**
     * 根据短链接获取指定日期内地区监控数据
     *
     * @param linkStatsReqDTO 查询参数
     * @return 地区访问统计列表
     */
    @Select("""
            SELECT
                tlls.province,
                SUM(tlls.cnt) AS cnt
            FROM t_link tl
            INNER JOIN t_link_locale_stats tlls
                ON tl.full_short_url = tlls.full_short_url
            WHERE tlls.full_short_url = #{param.fullShortUrl}
              AND tl.gid = #{param.gid}
              AND tl.del_flag = '0'
              AND tl.enable_status = #{param.enableStatus}
              AND tlls.date BETWEEN #{param.startDate} AND #{param.endDate}
            GROUP BY tlls.full_short_url, tl.gid, tlls.province
            """)
    List<LinkLocaleStatsDO> listLocaleByShortLink(@Param("param") LinkStatsReqDTO linkStatsReqDTO);

    /**
     * 根据分组获取指定日期内地区监控数据
     *
     * @param groupStatsReqDTO 查询参数
     * @return 地区访问统计列表
     */
    @Select("""
            SELECT
                tlls.province,
                SUM(tlls.cnt) AS cnt
            FROM
                t_link tl
                INNER JOIN t_link_locale_stats tlls
                    ON tl.full_short_url = tlls.full_short_url
            WHERE
                tl.gid = #{param.gid}
                AND tl.del_flag = '0'
                AND tl.enable_status = '0'
                AND tlls.date BETWEEN #{param.startDate} AND #{param.endDate}
            GROUP BY
                tl.gid,
                tlls.province
            """)
    List<LinkLocaleStatsDO> listLocaleByGroup(@Param("param") GroupStatsReqDTO groupStatsReqDTO);

}
