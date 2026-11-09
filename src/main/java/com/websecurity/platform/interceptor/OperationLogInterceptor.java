package com.websecurity.platform.interceptor;

import com.websecurity.platform.entity.OperationLog;
import com.websecurity.platform.entity.SysUser;
import com.websecurity.platform.mapper.OperationLogMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.LocalDateTime;

@Component
public class OperationLogInterceptor implements HandlerInterceptor {

    private static final Logger log =
            LoggerFactory.getLogger(OperationLogInterceptor.class);

    private static final String START_TIME =
            "operationLogStartTime";

    private static final String USER_SNAPSHOT =
            "operationLogUserSnapshot";

    private final OperationLogMapper operationLogMapper;

    public OperationLogInterceptor(
            OperationLogMapper operationLogMapper) {
        this.operationLogMapper = operationLogMapper;
    }

    /**
     * 请求处理前记录开始时间和当前用户信息。
     */
    @Override
    public boolean preHandle(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler) {

        String method = request.getMethod();

        // 只记录会改变系统数据的请求
        if (!isWriteMethod(method)) {
            return true;
        }

        request.setAttribute(
                START_TIME,
                System.currentTimeMillis()
        );

        SysUser user = getSessionUser(request);

        if (user != null) {
            request.setAttribute(
                    USER_SNAPSHOT,
                    new UserSnapshot(
                            user.getId(),
                            user.getUsername(),
                            user.getRole()
                    )
            );
        }

        return true;
    }

    /**
     * 请求完成后保存操作日志。
     */
    @Override
    public void afterCompletion(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler,
            Exception exception) {

        Object startObject = request.getAttribute(START_TIME);

        // 非写操作不记录
        if (!(startObject instanceof Long startTime)) {
            return;
        }

        try {
            String method = request.getMethod();
            String uri = request.getRequestURI();

            UserSnapshot userSnapshot =
                    (UserSnapshot) request.getAttribute(USER_SNAPSHOT);

            // 登录成功后，用户是在请求处理中才建立Session的
            // 因此需要在请求结束时再次读取
            if (userSnapshot == null) {
                SysUser currentUser = getSessionUser(request);

                if (currentUser != null) {
                    userSnapshot = new UserSnapshot(
                            currentUser.getId(),
                            currentUser.getUsername(),
                            currentUser.getRole()
                    );
                }
            }

            OperationLog operationLog = new OperationLog();

            if (userSnapshot != null) {
                operationLog.setUserId(userSnapshot.userId());
                operationLog.setUsername(userSnapshot.username());
                operationLog.setRole(userSnapshot.role());
            }

            operationLog.setOperationDesc(
                    describeOperation(method, uri)
            );

            operationLog.setHttpMethod(method);
            operationLog.setRequestUri(uri);
            operationLog.setIpAddress(
                    limitLength(request.getRemoteAddr(), 64)
            );
            operationLog.setResponseStatus(response.getStatus());

            operationLog.setDurationMs(
                    Math.max(0, System.currentTimeMillis() - startTime)
            );

            operationLog.setCreatedAt(LocalDateTime.now());

            operationLogMapper.insert(operationLog);

        } catch (Exception e) {
            // 日志保存失败不能影响原业务请求
            log.error("保存系统操作日志失败", e);
        }
    }

    /**
     * 判断是否为写操作。
     */
    private boolean isWriteMethod(String method) {
        return "POST".equalsIgnoreCase(method)
                || "PUT".equalsIgnoreCase(method)
                || "PATCH".equalsIgnoreCase(method)
                || "DELETE".equalsIgnoreCase(method);
    }

    /**
     * 获取当前Session用户。
     */
    private SysUser getSessionUser(HttpServletRequest request) {
        HttpSession session = request.getSession(false);

        if (session == null) {
            return null;
        }

        Object userObject = session.getAttribute("loginUser");

        if (userObject instanceof SysUser user) {
            return user;
        }

        return null;
    }

    /**
     * 根据请求地址生成操作描述。
     */
    private String describeOperation(String method, String uri) {

        if (uri.startsWith("/api/user/login")) {
            return "用户登录";
        }

        if (uri.startsWith("/api/user/logout")) {
            return "用户退出登录";
        }

        if (uri.startsWith("/api/user/register")) {
            return "用户注册";
        }

        if (uri.startsWith("/api/admin/users")) {
            return "管理员用户管理操作";
        }

        if (uri.startsWith("/api/admin/experiments")) {
            return "管理员实验资源管理操作";
        }

        if (uri.startsWith("/api/admin/vulnerabilities")
                || uri.startsWith("/api/admin/categories")) {
            return "管理员漏洞知识管理操作";
        }

        if (uri.contains("/claim")) {
            return "学生领取实验任务";
        }

        if (uri.contains("/start")) {
            return "学生启动实验任务";
        }

        if (uri.contains("/submit")) {
            return "学生提交实验报告";
        }

        if (uri.startsWith("/api/admin/submissions")) {
            return "管理员审核实验报告";
        }

        if (uri.contains("/attachment")) {
            return "实验报告附件操作";
        }

        return "系统数据写入操作";
    }

    /**
     * 限制字段长度，避免数据库插入异常。
     */
    private String limitLength(String value, int maxLength) {
        if (value == null) {
            return null;
        }

        if (value.length() <= maxLength) {
            return value;
        }

        return value.substring(0, maxLength);
    }

    /**
     * 请求开始时的用户快照。
     */
    private record UserSnapshot(
            Long userId,
            String username,
            String role
    ) {
    }
}