package dev.haotangyuan.shortlink.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import dev.haotangyuan.shortlink.dto.req.GroupStatsAccessRecordReqDTO;
import dev.haotangyuan.shortlink.dto.req.GroupStatsReqDTO;
import dev.haotangyuan.shortlink.dto.req.LinkStatsAccessRecordReqDTO;
import dev.haotangyuan.shortlink.dto.req.LinkStatsReqDTO;
import dev.haotangyuan.shortlink.dto.resp.LinkStatsAccessRecordRespDTO;
import dev.haotangyuan.shortlink.dto.resp.LinkStatsRespDTO;

/**
 * 访问统计接口层
 * @author: haotangyuan
 */
public interface LinkStatsService {

    /**
     * 获取单个短链接监控数据
     * @param linkStatsReqDTO 获取短链接监控数据入参
     * @return 短链接监控数据
     */
    LinkStatsRespDTO oneShortLinkStats(LinkStatsReqDTO linkStatsReqDTO);

    /**
     * 访问单个短链接指定时间内访问记录监控数据
     * @param linkStatsAccessRecordReqDTO 获取短链接监控访问记录数据入参
     * @return 访问记录监控数据
     */
    IPage<LinkStatsAccessRecordRespDTO> shortLinkStatsAccessRecord(LinkStatsAccessRecordReqDTO linkStatsAccessRecordReqDTO);

    /**
     * 获取分组短链接监控数据
     * @param groupStatsReqDTO 获取分组短链接监控数据入参
     * @return 短链接监控数据
     */
    LinkStatsRespDTO groupShortLinkStats(GroupStatsReqDTO groupStatsReqDTO);

    /**
     * 访问分组短链接指定时间内访问记录监控数据
     * @param groupStatsAccessRecordReqDTO 获取分组短链接监控访问记录数据入参
     * @return 访问记录监控数据
     */
    IPage<LinkStatsAccessRecordRespDTO> groupShortLinkStatsAccessRecord(GroupStatsAccessRecordReqDTO groupStatsAccessRecordReqDTO);
}
