package dev.haotangyuan.shortlink.service;

import dev.haotangyuan.shortlink.dto.req.UserLoginReqDTO;
import dev.haotangyuan.shortlink.dto.req.UserRegisterReqDTO;
import dev.haotangyuan.shortlink.dto.req.UserUpdateReqDTO;
import dev.haotangyuan.shortlink.vo.UserLoginVO;
import dev.haotangyuan.shortlink.vo.UserVO;

/**
 * 用户接口层
 * @author: haotangyuan
 */
public interface UserService {
    /**
     * 根据用户名查询用户信息
     * @param username 用户名
     * @return UserVO 用户返回实体
     */
    UserVO getByUsername(String username);

    /**
     * 查看用户名是否存在
     * @param username 用户名
     * @return Boolean
     */
    Boolean existsByUsername(String username);

    /**
     * 注册用户
     * @param userRegisterReqDTO
     * @return void
     */
    void register(UserRegisterReqDTO userRegisterReqDTO);

    /**
     * 修改用户信息
     * @param userUpdateReqDTO
     * @return void
     */
    void updateByUsername(UserUpdateReqDTO userUpdateReqDTO);

    /**
     * 用户登录
     * @param userLoginReqDTO
     * @return UserLoginVO
     */
    UserLoginVO login(UserLoginReqDTO userLoginReqDTO);

    /**
     * 检查用户是否登录
     * @param username
     * @param token
     * @return Boolean
     */
    Boolean checkLogin(String username, String token);

    /**
     * 退出登录
     * @param username
     * @param token
     * @return
     */
    void logout(String username, String token);
}
