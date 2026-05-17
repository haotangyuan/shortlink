package dev.haotangyuan.shortlink.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import dev.haotangyuan.shortlink.common.biz.user.GroupOwnershipVerifier;
import dev.haotangyuan.shortlink.dao.entity.LinkDO;
import dev.haotangyuan.shortlink.dao.mapper.LinkMapper;
import dev.haotangyuan.shortlink.dto.req.RecycleBinLinkPageReqDTO;
import dev.haotangyuan.shortlink.dto.req.RecycleBinRemoveReqDTO;
import dev.haotangyuan.shortlink.dto.req.RecycleBinRestoreReqDTO;
import dev.haotangyuan.shortlink.dto.req.RecycleBinSaveReqDTO;
import dev.haotangyuan.shortlink.dto.resp.LinkPageRespDTO;
import dev.haotangyuan.shortlink.service.RecycleBinService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import static dev.haotangyuan.shortlink.common.constant.RedisKeyConstant.GOTO_IS_NULL_SHORT_LINK_KEY;
import static dev.haotangyuan.shortlink.common.constant.RedisKeyConstant.GOTO_SHORT_LINK_KEY;

/**
 * 回收站管理接口实现层
 * @author: haotangyuan
 */
@Service
@RequiredArgsConstructor
public class RecycleBinServiceImpl extends ServiceImpl<LinkMapper, LinkDO> implements RecycleBinService {

    private final StringRedisTemplate stringRedisTemplate;
    private final GroupOwnershipVerifier groupOwnershipService;

    @Override
    public void saveRecycledBin(RecycleBinSaveReqDTO recycleBinSaveReqDTO) {
        groupOwnershipService.assertOwnedByCurrentUser(recycleBinSaveReqDTO.getGid());
        LambdaUpdateWrapper<LinkDO> updateWrapper = Wrappers.lambdaUpdate(LinkDO.class)
                .eq(LinkDO::getFullShortUrl, recycleBinSaveReqDTO.getFullShortUrl())
                .eq(LinkDO::getGid, recycleBinSaveReqDTO.getGid())
                .eq(LinkDO::getEnableStatus, 0)
                .eq(LinkDO::getDelFlag, 0);
        LinkDO linkDO = LinkDO.builder()
                .enableStatus(1)
                .build();
        baseMapper.update(linkDO, updateWrapper);
        stringRedisTemplate.delete(
                String.format(GOTO_SHORT_LINK_KEY, recycleBinSaveReqDTO.getFullShortUrl())
        );
    }

    @Override
    public IPage<LinkPageRespDTO> pageRecycleBinLink(RecycleBinLinkPageReqDTO recycleBinLinkPageReqDTO) {
        groupOwnershipService.assertAllOwnedByCurrentUser(recycleBinLinkPageReqDTO.getGidList());
        LambdaQueryWrapper<LinkDO> queryWrapper = Wrappers.lambdaQuery(LinkDO.class)
                .eq(LinkDO::getDelFlag, 0)
                .in(LinkDO::getGid, recycleBinLinkPageReqDTO.getGidList())
                .eq(LinkDO::getEnableStatus, 1)
                .orderByDesc(LinkDO::getUpdateTime);
        IPage<LinkDO> resultPage = baseMapper.selectPage(recycleBinLinkPageReqDTO, queryWrapper);
        return resultPage.convert(each -> {
            LinkPageRespDTO bean = BeanUtil.toBean(each, LinkPageRespDTO.class);
            bean.setDomain("http://" + bean.getDomain());
            return bean;
        });
    }

    @Override
    public void restoreLink(RecycleBinRestoreReqDTO recycleBinRestoreReqDTO) {
        groupOwnershipService.assertOwnedByCurrentUser(recycleBinRestoreReqDTO.getGid());
        LambdaUpdateWrapper<LinkDO> updateWrapper = Wrappers.lambdaUpdate(LinkDO.class)
                .eq(LinkDO::getFullShortUrl, recycleBinRestoreReqDTO.getFullShortUrl())
                .eq(LinkDO::getGid, recycleBinRestoreReqDTO.getGid())
                .eq(LinkDO::getEnableStatus, 1)
                .eq(LinkDO::getDelFlag, 0);
        LinkDO linkDO = LinkDO.builder()
                .enableStatus(0)
                .build();
        baseMapper.update(linkDO, updateWrapper);
        stringRedisTemplate.delete(
                String.format(GOTO_IS_NULL_SHORT_LINK_KEY, recycleBinRestoreReqDTO.getFullShortUrl())
        );
    }

    @Override
    public void removeLink(RecycleBinRemoveReqDTO recycleBinRemoveReqDTO) {
        groupOwnershipService.assertOwnedByCurrentUser(recycleBinRemoveReqDTO.getGid());
        LambdaUpdateWrapper<LinkDO> updateWrapper = Wrappers.lambdaUpdate(LinkDO.class)
                .eq(LinkDO::getFullShortUrl, recycleBinRemoveReqDTO.getFullShortUrl())
                .eq(LinkDO::getGid, recycleBinRemoveReqDTO.getGid())
                .eq(LinkDO::getEnableStatus, 1)
                .eq(LinkDO::getDelFlag, 0);
        LinkDO delLinkDO = LinkDO.builder()
                .delTime(System.currentTimeMillis())
                .build();
        delLinkDO.setDelFlag(1);
        baseMapper.update(delLinkDO, updateWrapper);
    }
}
