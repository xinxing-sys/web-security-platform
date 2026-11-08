
package com.websecurity.platform.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.websecurity.platform.common.ApiResponse;
import com.websecurity.platform.dto.AdminUserVO;
import com.websecurity.platform.dto.UserQueryRequest;
import com.websecurity.platform.dto.UserRoleUpdateRequest;
import com.websecurity.platform.dto.UserStatusUpdateRequest;
import com.websecurity.platform.entity.SysUser;
import com.websecurity.platform.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {

    private final UserService userService;

    public AdminUserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * 管理员分页查询用户
     */
    @GetMapping
    public ApiResponse<Map<String, Object>> queryUsers(
            @Valid @ModelAttribute UserQueryRequest request) {

        try {

            IPage<SysUser> page =
                    userService.queryUsers(request);

            List<AdminUserVO> records = new ArrayList<>();

            for (SysUser user : page.getRecords()) {
                records.add(convertToVO(user));
            }

            Map<String, Object> result = new LinkedHashMap<>();

            result.put("pageNum", page.getCurrent());
            result.put("pageSize", page.getSize());
            result.put("total", page.getTotal());
            result.put("totalPages", page.getPages());
            result.put("records", records);

            return ApiResponse.success(
                    "查询用户列表成功",
                    result
            );

        } catch (IllegalArgumentException e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    /**
     * 管理员查看用户详情
     */
    @GetMapping("/{id}")
    public ApiResponse<AdminUserVO> getUserDetail(
            @PathVariable Long id) {

        try {

            SysUser user = userService.findById(id);

            AdminUserVO vo = convertToVO(user);

            return ApiResponse.success(
                    "获取用户详情成功",
                    vo
            );

        } catch (IllegalArgumentException e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    /**
     * 将用户实体转换为安全的VO对象
     */
    private AdminUserVO convertToVO(SysUser user) {

        AdminUserVO vo = new AdminUserVO();

        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setEmail(user.getEmail());
        vo.setRole(user.getRole());
        vo.setStatus(user.getStatus());
        vo.setCreatedAt(user.getCreatedAt());
        vo.setUpdatedAt(user.getUpdatedAt());

        return vo;
    }

    /**
     * 修改用户状态
     * PUT /api/admin/users/{id}/status
     */
    @PutMapping("/{id}/status")
    public ApiResponse<?> updateUserStatus(
            @PathVariable Long id,
            @Valid @RequestBody UserStatusUpdateRequest request,
            HttpServletRequest httpRequest) {

        try {
            HttpSession session = httpRequest.getSession(false);

            if (session == null) {
                return ApiResponse.error("登录状态已失效");
            }

            SysUser loginUser = (SysUser) session.getAttribute("loginUser");

            if (loginUser == null) {
                return ApiResponse.error("请先登录");
            }

            userService.updateUserStatus(
                    id,
                    request.getStatus(),
                    loginUser.getId()
            );

            AdminUserVO userVO = convertToVO(userService.findById(id));

            return ApiResponse.success("用户状态修改成功", userVO);

        } catch (IllegalArgumentException e) {
            return ApiResponse.error(e.getMessage());
        } catch (Exception e) {
            return ApiResponse.error("修改用户状态失败");
        }
    }


    /**
     * 修改用户角色
     * PUT /api/admin/users/{id}/role
     */
    @PutMapping("/{id}/role")
    public ApiResponse<?> updateUserRole(
            @PathVariable Long id,
            @Valid @RequestBody UserRoleUpdateRequest request,
            HttpServletRequest httpRequest) {

        try {
            HttpSession session = httpRequest.getSession(false);

            if (session == null) {
                return ApiResponse.error("登录状态已失效");
            }

            SysUser loginUser = (SysUser) session.getAttribute("loginUser");

            if (loginUser == null) {
                return ApiResponse.error("请先登录");
            }

            userService.updateUserRole(
                    id,
                    request.getRole(),
                    loginUser.getId()
            );

            AdminUserVO userVO = convertToVO(userService.findById(id));

            return ApiResponse.success("用户角色修改成功", userVO);

        } catch (IllegalArgumentException e) {
            return ApiResponse.error(e.getMessage());
        } catch (Exception e) {
            return ApiResponse.error("修改用户角色失败");
        }
    }
}
