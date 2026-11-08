package com.websecurity.platform.controller;

import com.websecurity.platform.common.ApiResponse;
import com.websecurity.platform.dto.LoginRequest;
import com.websecurity.platform.dto.RegisterRequest;
import com.websecurity.platform.entity.SysUser;
import com.websecurity.platform.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/user")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * 学生注册
     */
    @PostMapping("/register")
    public ApiResponse<Map<String, Object>> register(
            @Valid @RequestBody RegisterRequest request) {

        try {
            SysUser user = userService.register(request);

            return ApiResponse.success(
                    "注册成功",
                    buildUserInfo(user)
            );

        } catch (IllegalArgumentException e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    /**
     * 用户登录
     */
    @PostMapping("/login")
    public ApiResponse<Map<String, Object>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {

        try {
            SysUser user = userService.login(request);

            // 创建会话并保存登录用户
            HttpSession oldSession = httpRequest.getSession(false);
            if (oldSession != null) {
                oldSession.invalidate();
            }

            HttpSession session = httpRequest.getSession(true);
            session.setAttribute("loginUser", user);
            session.setMaxInactiveInterval(30 * 60);

            return ApiResponse.success(
                    "登录成功",
                    buildUserInfo(user)
            );

        } catch (IllegalArgumentException e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    /**
     * 用户退出
     */
    @PostMapping("/logout")
    public ApiResponse<Void> logout(
            HttpServletRequest request) {

        HttpSession session = request.getSession(false);

        if (session != null) {
            session.invalidate();
        }

        return ApiResponse.success("退出登录成功", null);
    }

    /**
     * 获取当前登录用户
     */
    @GetMapping("/me")
    public ApiResponse<Map<String, Object>> currentUser(
            HttpServletRequest request) {

        HttpSession session = request.getSession(false);

        if (session == null) {
            return ApiResponse.error("登录状态已失效");
        }

        SysUser user = (SysUser) session.getAttribute("loginUser");

        if (user == null) {
            return ApiResponse.error("请先登录");
        }

        return ApiResponse.success(
                "获取用户信息成功",
                buildUserInfo(user)
        );
    }

    /**
     * 构造安全的用户信息
     */
    private Map<String, Object> buildUserInfo(SysUser user) {

        Map<String, Object> result = new HashMap<>();

        result.put("id", user.getId());
        result.put("username", user.getUsername());
        result.put("nickname", user.getNickname());
        result.put("email", user.getEmail());
        result.put("role", user.getRole());

        return result;
    }
}