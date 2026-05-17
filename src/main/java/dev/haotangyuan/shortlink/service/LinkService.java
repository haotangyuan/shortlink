package dev.haotangyuan.shortlink.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import dev.haotangyuan.shortlink.dao.entity.LinkDO;
import dev.haotangyuan.shortlink.dto.biz.LinkStatsRecordDTO;
import dev.haotangyuan.shortlink.dto.req.LinkBatchCreateReqDTO;
import dev.haotangyuan.shortlink.dto.req.LinkCreateReqDTO;
import dev.haotangyuan.shortlink.dto.req.LinkPageReqDTO;
import dev.haotangyuan.shortlink.dto.req.LinkUpdateReqDTO;
import dev.haotangyuan.shortlink.dto.resp.GroupLinkCountQueryRespDTO;
import dev.haotangyuan.shortlink.dto.resp.LinkBatchCreateRespDTO;
import dev.haotangyuan.shortlink.dto.resp.LinkCreateRespDTO;
import dev.haotangyuan.shortlink.dto.resp.LinkPageRespDTO;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;

import java.util.List;

/**
 * 短链接接口层
 * @author: haotangyuan
 */
public interface LinkService extends IService<LinkDO> {

    /**
     * 创建短链接
     * @param linkCreateReqDTO
     * @return
     */
    LinkCreateRespDTO createLink(LinkCreateReqDTO linkCreateReqDTO);

    /**
     * 更新短链接
     * @param linkUpdateReqDTO 短链接更新请求参数
     */
    void updateLink(LinkUpdateReqDTO linkUpdateReqDTO);

    /**
     * 短链接分页查询
     * @param linkPageReqDTO 分页请求参数
     * @return IPage<LinkPageRespDTO>
     */
    IPage<LinkPageRespDTO> pageLink(LinkPageReqDTO linkPageReqDTO);

    /**
     * 查询分组内短链接数量
     * @param gidList 分组标识列表
     * @return List<GroupLinkCountQueryRespDTO>
     */
    List<GroupLinkCountQueryRespDTO> listGroupLinkCount(List<String> gidList);

    /**
     * 根据短链接还原原始链接
     * @param shortUri 短链接后缀
     * @param request HttpServerRequest
     * @param response HttpServerResponse
     */
    void restoreUrl(String shortUri, ServletRequest request, ServletResponse response);

    /**
     * 批量创建短链接
     * @param linkBatchCreateReqDTO 短链接批量创建请求参数
     * @return LinkBatchCreateRespDTO
     */
    LinkBatchCreateRespDTO batchCreateLink(LinkBatchCreateReqDTO linkBatchCreateReqDTO);

    /**
     * 短链接统计
     * @param linkStatsRecordDTO 短链接统计实体参数
     */
    void linkStats(LinkStatsRecordDTO linkStatsRecordDTO);
}
