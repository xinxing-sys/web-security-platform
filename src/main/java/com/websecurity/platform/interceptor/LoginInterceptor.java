
package com.websecurity.platform.interceptor;

import tools.jackson.databind.ObjectMapper;
import com.websecurity.platform.common.ApiResponse;
import com.websecurity.platform.entity.SysUser;
import com.websecurity.platform.mapper.SysUserMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;

@Component
public class LoginInterceptor implements HandlerInterceptor {

    private final ObjectMapper objectMapper;
    private final SysUserMapper userMapper;

    public LoginInterceptor(
            ObjectMapper objectMapper,
            SysUserMapper userMapper) {
        this.objectMapper = objectMapper;
        this.userMapper = userMapper;
    }

    @Override
    public boolean preHandle(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler) throws IOException {

        HttpSession session = request.getSession(false);

        SysUser sessionUser = session == null
                ? null
                : (SysUser) session.getAttribute("loginUser");

        response.setContentType("application/json;charset=UTF-8");

        // 检查是否已经登录
        if (sessionUser == null || sessionUser.getId() == null) {
            writeError(
                    response,
                    HttpServletResponse.SC_UNAUTHORIZED,
                    "请先登录"
            );
            return false;
        }

        // 每次请求都从数据库读取最新用户信息
        SysUser currentUser = userMapper.selectById(sessionUser.getId());

        // 用户不存在
        if (currentUser == null) {
            session.invalidate();

            writeError(
                    response,
                    HttpServletResponse.SC_UNAUTHORIZED,
                    "用户不存在，请重新登录"
            );
            return false;
        }

        // 用户已被禁用
        if (currentUser.getStatus() == null
                || currentUser.getStatus() != 1) {

            session.invalidate();

            writeError(
                    response,
                    HttpServletResponse.SC_UNAUTHORIZED,
                    "账号已被禁用，请联系管理员"
            );
            return false;
        }

        // 更新Session中的用户信息，确保角色和资料是最新的
        currentUser.setPassword(null);
        session.setAttribute("loginUser", currentUser);

        // 管理员接口必须由当前数据库中的管理员访问
        if (request.getRequestURI().startsWith("/api/admin/")
                && !"ADMIN".equals(currentUser.getRole())) {

            writeError(
                    response,
                    HttpServletResponse.SC_FORBIDDEN,
                    "没有管理员权限"
            );
            return false;
        }

        return true;
    }

    /**
     * 统一返回拦截错误
     */
    private void writeError(
            HttpServletResponse response,
            int status,
            String message) throws IOException {

        response.setStatus(status);

        objectMapper.writeValue(
                response.getWriter(),
                ApiResponse.error(message)
        );
    }
}
