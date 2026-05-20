package dev.haotangyuan.shortlink.controller.core;

import com.baomidou.mybatisplus.core.metadata.IPage;
import dev.haotangyuan.shortlink.common.convention.result.Result;
import dev.haotangyuan.shortlink.common.convention.result.Results;
import dev.haotangyuan.shortlink.dto.req.LinkBatchCreateReqDTO;
import dev.haotangyuan.shortlink.dto.req.LinkCreateReqDTO;
import dev.haotangyuan.shortlink.dto.req.LinkPageReqDTO;
import dev.haotangyuan.shortlink.dto.req.LinkUpdateReqDTO;
import dev.haotangyuan.shortlink.vo.GroupLinkCountQueryVO;
import dev.haotangyuan.shortlink.vo.LinkBatchCreateVO;
import dev.haotangyuan.shortlink.vo.LinkCreateVO;
import dev.haotangyuan.shortlink.vo.LinkPageVO;
import dev.haotangyuan.shortlink.service.LinkService;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 短链接控制层
 * @author: haotangyuan
 */
@RestController
@RequiredArgsConstructor
public class LinkController {

    private final LinkService linkService;


    @GetMapping("/{shortUri}")
    public void restoreUrl(@PathVariable String shortUri, ServletRequest request, ServletResponse response) {
        linkService.restoreUrl(shortUri, request, response);
    }

    /**
     * 创建短链接
     * @return Result
     */
    @PostMapping("/api/short-link/v1/create")
    public Result<LinkCreateVO> createLink(@RequestBody LinkCreateReqDTO linkCreateReqDTO) {
        return Results.success(linkService.createLink(linkCreateReqDTO));
    }

    /**
     * 批量创建短链接
     */
    @PostMapping("/api/short-link/v1/create/batch")
    public Result<LinkBatchCreateVO> batchCreateShortLink(@RequestBody LinkBatchCreateReqDTO linkBatchCreateReqDTO) {
        return Results.success(linkService.batchCreateLink(linkBatchCreateReqDTO));
    }

    /**
     * 修改短链接
     * @param linkUpdateReqDTO 短链接更新请求参数
     * @return Result<Void>
     */
    @PostMapping("/api/short-link/v1/update")
    public Result<Void> updateLink(@RequestBody LinkUpdateReqDTO linkUpdateReqDTO) {
        linkService.updateLink(linkUpdateReqDTO);
        return Results.success();
    }

    /**
     * 短链接分页查询
     * @param linkPageReqDTO 分页请求参数
     * @return Result<IPage<LinkPageVO>>
     */
    @GetMapping("/api/short-link/v1/page")
    public Result<IPage<LinkPageVO>> pageLink(LinkPageReqDTO linkPageReqDTO) {
        return Results.success(linkService.pageLink(linkPageReqDTO));
    }

    /**
     * 查询分组内短链接数量
     * @return Result<List<GroupLinkCountQueryVO>>
     */
    @GetMapping("/api/short-link/v1/count")
    public Result<List<GroupLinkCountQueryVO>> listGroupLinkCount(@RequestParam("requestParam") List<String> gidList) {
        return Results.success(linkService.listGroupLinkCount(gidList));
    }
}
