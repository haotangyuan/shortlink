package dev.haotangyuan.shortlink.controller.admin;

import com.baomidou.mybatisplus.core.metadata.IPage;
import dev.haotangyuan.shortlink.common.convention.result.Result;
import dev.haotangyuan.shortlink.common.convention.result.Results;
import dev.haotangyuan.shortlink.dto.req.GroupStatsAccessRecordReqDTO;
import dev.haotangyuan.shortlink.dto.req.GroupStatsReqDTO;
import dev.haotangyuan.shortlink.dto.req.LinkStatsAccessRecordReqDTO;
import dev.haotangyuan.shortlink.dto.req.LinkStatsReqDTO;
import dev.haotangyuan.shortlink.vo.LinkStatsAccessRecordVO;
import dev.haotangyuan.shortlink.vo.LinkStatsVO;
import dev.haotangyuan.shortlink.service.LinkStatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 访问统计控制器
 * @author: haotangyuan
 */
@RestController
@RequiredArgsConstructor
public class LinkStatsAdminController {

    private final LinkStatsService linkStatsService;

    /**
     * 访问单个短链接指定时间内监控数据
     * @param linkStatsReqDTO 获取短链接监控数据入参
     * @return 短链接监控数据
     */
    @GetMapping("/api/short-link/admin/v1/stats")
    public Result<LinkStatsVO> shortLinkStats(LinkStatsReqDTO linkStatsReqDTO) {
        return Results.success(linkStatsService.oneShortLinkStats(linkStatsReqDTO));
    }

    /**
     * 访问分组短链接指定时间内监控数据
     * @param groupStatsReqDTO 获取短链接监控数据入参
     * @return 短链接监控数据
     */
    @GetMapping("/api/short-link/admin/v1/stats/group")
    public Result<LinkStatsVO> groupShortLinkStats(GroupStatsReqDTO groupStatsReqDTO) {
        return Results.success(linkStatsService.groupShortLinkStats(groupStatsReqDTO));
    }

    /**
     * 访问单个短链接指定时间内访问记录监控数据
     * @param linkStatsAccessRecordReqDTO 获取短链接监控访问记录数据入参
     * @return 访问记录监控数据
     */
    @GetMapping("/api/short-link/admin/v1/stats/access-record")
    public Result<IPage<LinkStatsAccessRecordVO>> shortLinkStatsAccessRecord(LinkStatsAccessRecordReqDTO linkStatsAccessRecordReqDTO) {
        return Results.success(linkStatsService.shortLinkStatsAccessRecord(linkStatsAccessRecordReqDTO));
    }

    /**
     * 访问分组短链接指定时间内访问记录监控数据
     * @param groupStatsAccessRecordReqDTO 获取分组短链接监控访问记录数据入参
     * @return 访问记录监控数据
     */
    @GetMapping("/api/short-link/admin/v1/stats/access-record/group")
    public Result<IPage<LinkStatsAccessRecordVO>> groupShortLinkStatsAccessRecord(GroupStatsAccessRecordReqDTO groupStatsAccessRecordReqDTO) {
        return Results.success(linkStatsService.groupShortLinkStatsAccessRecord(groupStatsAccessRecordReqDTO));
    }
}
