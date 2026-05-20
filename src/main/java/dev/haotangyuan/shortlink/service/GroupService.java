package dev.haotangyuan.shortlink.service;

import dev.haotangyuan.shortlink.dto.req.GroupSortReqDTO;
import dev.haotangyuan.shortlink.dto.req.GroupUpdateReqDTO;
import dev.haotangyuan.shortlink.vo.GroupVO;

import java.util.List;

/**
 * 短链接分组接口层
 *
 * @author: haotangyuan
 */
public interface GroupService {
    /**
     * 新增短链接分组
     *
     * @param groupName 短链接分组名
     * @return void
     */
    void saveGroup(String groupName);

    /**
     * 新增短链接分组
     *
     * @param username  用户名
     * @param groupName 短链接分组名
     * @return void
     */
    void saveGroup(String username, String groupName);

    /**
     * 查询短链接分组集合
     *
     * @return List<GroupVO>
     */
    List<GroupVO> listGroup();

    /**
     * 修改短链接分组
     *
     * @param groupUpdateReqDTO 短链接分组参数
     */
    void updateGroup(GroupUpdateReqDTO groupUpdateReqDTO);

    /**
     * 删除短链接分组
     *
     * @param gid 短链接分组标识
     */
    void deleteGroup(String gid);

    /**
     * 短链接分组排序
     *
     * @param groupSortReqDTOs
     */
    void sortGroup(List<GroupSortReqDTO> groupSortReqDTOs);
}
