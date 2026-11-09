package com.websecurity.platform.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.websecurity.platform.common.ApiResponse;
import com.websecurity.platform.entity.OperationLog;
import com.websecurity.platform.entity.SysUser;
import com.websecurity.platform.mapper.OperationLogMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/logs")
public class OperationLogController {

    private final OperationLogMapper operationLogMapper;

    public OperationLogController(
            OperationLogMapper operationLogMapper) {
        this.operationLogMapper = operationLogMapper;
    }

    /**
     * 管理员分页查询系统操作日志
     *
     * GET /api/admin/logs
     */
    @GetMapping
    public ApiResponse<Map<String, Object>> query(
            @RequestParam(required = false, defaultValue = "1")
            Integer pageNum,

            @RequestParam(required = false, defaultValue = "10")
            Integer pageSize,

            @RequestParam(required = false)
            String username,

            @RequestParam(required = false)
            String operation,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime startTime,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime endTime,

            HttpServletRequest request) {

        try {
            // 只允许管理员查询
            if (!isAdmin(request)) {
                return ApiResponse.error("只有管理员可以查看操作日志");
            }

            // 限制分页参数，防止一次查询过多数据
            int current = pageNum == null || pageNum < 1
                    ? 1 : pageNum;

            int size = pageSize == null || pageSize < 1
                    ? 10 : Math.min(pageSize, 100);

            if (startTime != null
                    && endTime != null
                    && startTime.isAfter(endTime)) {
                return ApiResponse.error("开始时间不能晚于结束时间");
            }

            LambdaQueryWrapper<OperationLog> wrapper =
                    new LambdaQueryWrapper<>();

            if (username != null && !username.isBlank()) {
                wrapper.like(
                        OperationLog::getUsername,
                        username.trim()
                );
            }

            if (operation != null && !operation.isBlank()) {
                wrapper.like(
                        OperationLog::getOperationDesc,
                        operation.trim()
                );
            }

            if (startTime != null) {
                wrapper.ge(
                        OperationLog::getCreatedAt,
                        startTime
                );
            }

            if (endTime != null) {
                wrapper.le(
                        OperationLog::getCreatedAt,
                        endTime
                );
            }

            wrapper.orderByDesc(OperationLog::getCreatedAt)
                    .orderByDesc(OperationLog::getId);

            Page<OperationLog> page =
                    new Page<>(current, size);

            IPage<OperationLog> result =
                    operationLogMapper.selectPage(page, wrapper);

            Map<String, Object> data = new LinkedHashMap<>();

            data.put("pageNum", result.getCurrent());
            data.put("pageSize", result.getSize());
            data.put("total", result.getTotal());
            data.put("totalPages", result.getPages());
            data.put("records", result.getRecords());

            return ApiResponse.success(
                    "查询操作日志成功",
                    data
            );

        } catch (Exception e) {
            e.printStackTrace();
            return ApiResponse.error("查询操作日志失败");
        }
    }

    /**
     * 判断当前用户是否为管理员
     */
    private boolean isAdmin(HttpServletRequest request) {
        HttpSession session = request.getSession(false);

        if (session == null) {
            return false;
        }

        Object userObject = session.getAttribute("loginUser");

        if (!(userObject instanceof SysUser user)) {
            return false;
        }

        return user.getId() != null
                && "ADMIN".equals(user.getRole());
    }
}