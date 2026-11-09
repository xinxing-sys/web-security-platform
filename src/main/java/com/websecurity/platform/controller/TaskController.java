package com.websecurity.platform.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.websecurity.platform.common.ApiResponse;
import com.websecurity.platform.entity.ExperimentTask;
import com.websecurity.platform.entity.SysUser;
import com.websecurity.platform.service.TaskService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    /**
     * 学生领取实验
     * POST /api/tasks/{experimentId}/claim
     */
    @PostMapping("/{experimentId}/claim")
    public ApiResponse<ExperimentTask> claim(
            @PathVariable Long experimentId,
            HttpServletRequest request) {

        try {
            SysUser user = getStudent(request);

            ExperimentTask task = taskService.claim(
                    user.getId(),
                    experimentId
            );

            return ApiResponse.success(
                    "领取实验成功",
                    task
            );

        } catch (IllegalArgumentException e) {
            return ApiResponse.error(e.getMessage());

        } catch (Exception e) {
            return ApiResponse.error("领取实验失败");
        }
    }

    /**
     * 查询当前学生的任务列表
     * GET /api/tasks
     */
    @GetMapping
    public ApiResponse<Map<String, Object>> queryMyTasks(
            @RequestParam(required = false, defaultValue = "1")
            Integer pageNum,

            @RequestParam(required = false, defaultValue = "10")
            Integer pageSize,

            @RequestParam(required = false)
            String status,

            HttpServletRequest request) {

        try {
            SysUser user = getStudent(request);

            IPage<ExperimentTask> page =
                    taskService.queryMyTasks(
                            user.getId(),
                            pageNum,
                            pageSize,
                            status
                    );

            Map<String, Object> result = new LinkedHashMap<>();

            result.put("pageNum", page.getCurrent());
            result.put("pageSize", page.getSize());
            result.put("total", page.getTotal());
            result.put("totalPages", page.getPages());
            result.put("records", page.getRecords());

            return ApiResponse.success(
                    "查询我的任务成功",
                    result
            );

        } catch (IllegalArgumentException e) {
            return ApiResponse.error(e.getMessage());

        } catch (Exception e) {
            return ApiResponse.error("查询我的任务失败");
        }
    }

    /**
     * 查询当前学生的任务详情
     * GET /api/tasks/{id}
     */
    @GetMapping("/{id}")
    public ApiResponse<ExperimentTask> getMyTask(
            @PathVariable Long id,
            HttpServletRequest request) {

        try {
            SysUser user = getStudent(request);

            ExperimentTask task = taskService.getMyTask(
                    user.getId(),
                    id
            );

            return ApiResponse.success(
                    "获取任务详情成功",
                    task
            );

        } catch (IllegalArgumentException e) {
            return ApiResponse.error(e.getMessage());

        } catch (Exception e) {
            return ApiResponse.error("获取任务详情失败");
        }
    }

    /**
     * 开始实验
     * PUT /api/tasks/{id}/start
     */
    @PutMapping("/{id}/start")
    public ApiResponse<ExperimentTask> start(
            @PathVariable Long id,
            HttpServletRequest request) {

        try {
            SysUser user = getStudent(request);

            ExperimentTask task = taskService.start(
                    user.getId(),
                    id
            );

            return ApiResponse.success(
                    "实验已开始",
                    task
            );

        } catch (IllegalArgumentException e) {
            return ApiResponse.error(e.getMessage());

        } catch (Exception e) {
            return ApiResponse.error("开始实验失败");
        }
    }

    /**
     * 获取当前登录学生
     */
    private SysUser getStudent(HttpServletRequest request) {

        HttpSession session = request.getSession(false);

        if (session == null) {
            throw new IllegalArgumentException("请先登录");
        }

        SysUser user = (SysUser) session.getAttribute("loginUser");

        if (user == null || user.getId() == null) {
            throw new IllegalArgumentException("登录状态已失效");
        }

        if (!"STUDENT".equals(user.getRole())) {
            throw new IllegalArgumentException(
                    "只有学生账号可以进行此操作"
            );
        }

        return user;
    }
}