package dev.haotangyuan.shortlink.controller.admin;

import cn.hutool.core.bean.BeanUtil;
import dev.haotangyuan.shortlink.common.convention.result.Result;
import dev.haotangyuan.shortlink.common.convention.result.Results;
import dev.haotangyuan.shortlink.dto.req.UserLoginReqDTO;
import dev.haotangyuan.shortlink.dto.req.UserRegisterReqDTO;
import dev.haotangyuan.shortlink.dto.req.UserUpdateReqDTO;
import dev.haotangyuan.shortlink.vo.UserActualVO;
import dev.haotangyuan.shortlink.vo.UserLoginVO;
import dev.haotangyuan.shortlink.vo.UserVO;
import dev.haotangyuan.shortlink.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 用户管理控制层
 *
 * @author: haotangyuan
 */
@RestController
@RequiredArgsConstructor
public class UserAdminController {

    private final UserService userService;

    /**
     * 根据用户名查找用户
     */
    @GetMapping("/api/short-link/admin/v1/user/{username}")
    public Result<UserVO> getUserByUsername(@PathVariable("username") String username) {
        return Results.success(userService.getByUsername(username));
    }

    /**
     * 根据用户名查找用户无脱敏
     */
    @GetMapping("/api/short-link/admin/v1/actual/user/{username}")
    public Result<UserActualVO> getActualUserByUsername(@PathVariable("username") String username) {
        return Results.success(BeanUtil.toBean(userService.getByUsername(username), UserActualVO.class));
    }

    /**
     * 查看用户名是否存在
     */
    @GetMapping("/api/short-link/admin/v1/user/exists")
    public Result<Boolean> existsByUsername(@RequestParam("username") String username) {
        return Results.success(userService.existsByUsername(username));
    }

    /**
     * 用户注册
     */
    @PostMapping("/api/short-link/admin/v1/user")
    public Result<Void> register(@RequestBody UserRegisterReqDTO userRegisterReqDTO) {
        userService.register(userRegisterReqDTO);
        return Results.success();
    }

    /**
     * 修改信息
     */
    @PutMapping("/api/short-link/admin/v1/user")
    public Result<Void> updateUser(@RequestBody UserUpdateReqDTO userUpdateReqDTO) {
        userService.updateByUsername(userUpdateReqDTO);
        return Results.success();
    }

    /**
     * 用户登录
     */
    @PostMapping("/api/short-link/admin/v1/user/login")
    public Result<UserLoginVO> login(@RequestBody UserLoginReqDTO userLoginReqDTO) {
        return Results.success(userService.login(userLoginReqDTO));
    }

    /**
     * 检查用户是否登录
     */
    @PostMapping("/api/short-link/admin/v1/user/check-login")
    public Result<Boolean> checkLogin(@RequestParam("username") String username, @RequestParam("token") String token) {
        return Results.success(userService.checkLogin(username, token));
    }

    /**
     * 退出登录
     */
    @DeleteMapping("/api/short-link/admin/v1/user/logout")
    public Result<Void> logout(@RequestParam("username") String username, @RequestParam("token") String token) {
        userService.logout(username, token);
        return Results.success();
    }
}
