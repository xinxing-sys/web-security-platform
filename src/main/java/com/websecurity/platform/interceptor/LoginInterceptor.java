package com.websecurity.platform.interceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.websecurity.platform.common.ApiResponse;
import com.websecurity.platform.entity.SysUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;

@Component
public class LoginInterceptor implements HandlerInterceptor {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public boolean preHandle(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler) throws IOException {

        HttpSession session = request.getSession(false);

        SysUser user = session == null
                ? null
                : (SysUser) session.getAttribute("loginUser");

        response.setContentType("application/json;charset=UTF-8");

        if (user == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            objectMapper.writeValue(
                    response.getWriter(),
                    ApiResponse.error("请先登录")
            );
            return false;
        }

        // 管理员接口必须由管理员访问
        if (request.getRequestURI().startsWith("/api/admin/")
                && !"ADMIN".equals(user.getRole())) {

            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            objectMapper.writeValue(
                    response.getWriter(),
                    ApiResponse.error("没有管理员权限")
            );
            return false;
        }

        return true;
    }
}