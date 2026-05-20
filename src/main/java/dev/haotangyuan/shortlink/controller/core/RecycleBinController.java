package dev.haotangyuan.shortlink.controller.core;

import com.baomidou.mybatisplus.core.metadata.IPage;
import dev.haotangyuan.shortlink.common.convention.result.Result;
import dev.haotangyuan.shortlink.common.convention.result.Results;
import dev.haotangyuan.shortlink.dto.req.RecycleBinLinkPageReqDTO;
import dev.haotangyuan.shortlink.dto.req.RecycleBinRemoveReqDTO;
import dev.haotangyuan.shortlink.dto.req.RecycleBinRestoreReqDTO;
import dev.haotangyuan.shortlink.dto.req.RecycleBinSaveReqDTO;
import dev.haotangyuan.shortlink.vo.LinkPageVO;
import dev.haotangyuan.shortlink.service.RecycleBinService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * 回收站控制层
 * @author: haotangyuan
 */
@RequiredArgsConstructor
@RestController
public class RecycleBinController {

    private final RecycleBinService recycleBinService;

    /**
     * 移至回收站
     * @param recycleBinSaveReqDTO 移至回收站保存请求参数
     * @return Result<Void>
     */
    @PostMapping("/api/short-link/v1/recycle-bin/save")
    public Result<Void> saveRecycledBin(@RequestBody RecycleBinSaveReqDTO recycleBinSaveReqDTO) {
        recycleBinService.saveRecycledBin(recycleBinSaveReqDTO);
        return Results.success();
    }

    /**
     * 短链接分页查询
     * @param recycleBinLinkPageReqDTO 分页请求参数
     * @return Result<IPage<LinkPageVO>>
     */
    @GetMapping("/api/short-link/v1/recycle-bin/page")
    public Result<IPage<LinkPageVO>> pageRecycledBinLink(RecycleBinLinkPageReqDTO recycleBinLinkPageReqDTO) {
        return Results.success(recycleBinService.pageRecycleBinLink(recycleBinLinkPageReqDTO));
    }

    /**
     * 恢复短链接
     * @param recycleBinRestoreReqDTO 恢复请求参数
     * @return Result<Void>
     */
    @PostMapping("/api/short-link/v1/recycle-bin/restore")
    public Result<Void> restoreLink(@RequestBody RecycleBinRestoreReqDTO recycleBinRestoreReqDTO) {
        recycleBinService.restoreLink(recycleBinRestoreReqDTO);
        return Results.success();
    }

    /**
     * 从回收站移除短链接
     * @param recycleBinRemoveReqDTO 回收站移除请求参数
     * @return Result<Void>
     */
    @PostMapping("/api/short-link/v1/recycle-bin/remove")
    public Result<Void> removeLink(@RequestBody RecycleBinRemoveReqDTO recycleBinRemoveReqDTO) {
        recycleBinService.removeLink(recycleBinRemoveReqDTO);
        return Results.success();
    }
}
