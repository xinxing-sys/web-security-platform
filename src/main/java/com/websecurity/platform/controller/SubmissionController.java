package com.websecurity.platform.controller;

import com.websecurity.platform.common.ApiResponse;
import com.websecurity.platform.dto.SubmitRequest;
import com.websecurity.platform.entity.ExperimentSubmit;
import com.websecurity.platform.entity.SysUser;
import com.websecurity.platform.service.SubmissionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class SubmissionController {

    private final SubmissionService submissionService;

    public SubmissionController(
            SubmissionService submissionService) {
        this.submissionService = submissionService;
    }

    /**
     * 学生提交实验报告
     *
     * POST /api/tasks/{taskId}/submit
     */
    @PostMapping("/api/tasks/{taskId}/submit")
    public ApiResponse<ExperimentSubmit> submit(
            @PathVariable Long taskId,
            @Valid @RequestBody SubmitRequest requestBody,
            HttpServletRequest request) {

        try {
            SysUser user = getStudent(request);

            ExperimentSubmit submission =
                    submissionService.submit(
                            user.getId(),
                            taskId,
                            requestBody.getContent()
                    );

            return ApiResponse.success(
                    "实验报告提交成功，等待教师审核",
                    submission
            );

        } catch (IllegalArgumentException e) {
            return ApiResponse.error(e.getMessage());

        } catch (Exception e) {
            return ApiResponse.error("实验报告提交失败");
        }
    }

    /**
     * 查询当前学生某个任务的报告历史
     *
     * GET /api/tasks/{taskId}/submissions
     */
    @GetMapping("/api/tasks/{taskId}/submissions")
    public ApiResponse<List<ExperimentSubmit>> queryMySubmissions(
            @PathVariable Long taskId,
            HttpServletRequest request) {

        try {
            SysUser user = getStudent(request);

            List<ExperimentSubmit> submissions =
                    submissionService.queryMySubmissions(
                            user.getId(),
                            taskId
                    );

            return ApiResponse.success(
                    "查询报告历史成功",
                    submissions
            );

        } catch (IllegalArgumentException e) {
            return ApiResponse.error(e.getMessage());

        } catch (Exception e) {
            return ApiResponse.error("查询报告历史失败");
        }
    }

    /**
     * 查询当前学生自己的报告详情
     *
     * GET /api/submissions/{id}
     */
    @GetMapping("/api/submissions/{id}")
    public ApiResponse<ExperimentSubmit> getMySubmission(
            @PathVariable Long id,
            HttpServletRequest request) {

        try {
            SysUser user = getStudent(request);

            ExperimentSubmit submission =
                    submissionService.getMySubmission(
                            user.getId(),
                            id
                    );

            return ApiResponse.success(
                    "获取报告详情成功",
                    submission
            );

        } catch (IllegalArgumentException e) {
            return ApiResponse.error(e.getMessage());

        } catch (Exception e) {
            return ApiResponse.error("获取报告详情失败");
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