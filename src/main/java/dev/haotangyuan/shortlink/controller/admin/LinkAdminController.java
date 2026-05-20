package dev.haotangyuan.shortlink.controller.admin;

import com.baomidou.mybatisplus.core.metadata.IPage;
import dev.haotangyuan.shortlink.common.convention.result.Result;
import dev.haotangyuan.shortlink.common.convention.result.Results;
import dev.haotangyuan.shortlink.dto.req.LinkBatchCreateReqDTO;
import dev.haotangyuan.shortlink.dto.req.LinkCreateReqDTO;
import dev.haotangyuan.shortlink.dto.req.LinkPageReqDTO;
import dev.haotangyuan.shortlink.dto.req.LinkUpdateReqDTO;
import dev.haotangyuan.shortlink.vo.LinkBatchCreateVO;
import dev.haotangyuan.shortlink.vo.LinkCreateVO;
import dev.haotangyuan.shortlink.vo.LinkPageVO;
import dev.haotangyuan.shortlink.service.LinkService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * 短链接控制层
 * @author: haotangyuan
 */
@RestController
@RequiredArgsConstructor
public class LinkAdminController {

    private final LinkService linkService;

    /**
     * 创建短链接
     * @param linkCreateReqDTO 短链接创建请求参数
     * @return Result
     */
    @PostMapping("/api/short-link/admin/v1/create")
    public Result<LinkCreateVO> createLink(@RequestBody LinkCreateReqDTO linkCreateReqDTO) {
        return Results.success(linkService.createLink(linkCreateReqDTO));
    }

    /**
     * 批量创建短链接
     */
    @PostMapping("/api/short-link/admin/v1/create/batch")
    public Result<LinkBatchCreateVO> batchCreateShortLink(@RequestBody LinkBatchCreateReqDTO linkBatchCreateReqDTO) {
        return Results.success(linkService.batchCreateLink(linkBatchCreateReqDTO));
    }

    /**
     * 修改短链接
     * @param linkUpdateReqDTO 短链接更新请求参数
     * @return Result<Void>
     */
    @PostMapping("/api/short-link/admin/v1/update")
    public Result<Void> updateLink(@RequestBody LinkUpdateReqDTO linkUpdateReqDTO) {
        linkService.updateLink(linkUpdateReqDTO);
        return Results.success();
    }

    /**
     * 短链接分页查询
     * @param linkPageReqDTO 分页请求参数
     * @return Result<IPage<LinkPageVO>>
     */
    @GetMapping("/api/short-link/admin/v1/page")
    public Result<IPage<LinkPageVO>> pageLink(LinkPageReqDTO linkPageReqDTO) {
        return Results.success(linkService.pageLink(linkPageReqDTO));
    }
}
