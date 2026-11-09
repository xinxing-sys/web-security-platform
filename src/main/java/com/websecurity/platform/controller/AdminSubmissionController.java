package com.websecurity.platform.controller;

import com.websecurity.platform.common.ApiResponse;
import com.websecurity.platform.dto.ReviewRequest;
import com.websecurity.platform.entity.ExperimentSubmit;
import com.websecurity.platform.entity.SysUser;
import com.websecurity.platform.service.AdminSubmissionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/submissions")
public class AdminSubmissionController {

    private final AdminSubmissionService adminSubmissionService;

    public AdminSubmissionController(
            AdminSubmissionService adminSubmissionService) {
        this.adminSubmissionService = adminSubmissionService;
    }

    /**
     * 查询报告列表
     *
     * GET /api/admin/submissions
     * GET /api/admin/submissions?status=PENDING
     */
    @GetMapping
    public ApiResponse<List<ExperimentSubmit>> querySubmissions(
            @RequestParam(required = false) String status,
            HttpServletRequest request) {

        try {
            getAdmin(request);

            List<ExperimentSubmit> submissions =
                    adminSubmissionService.querySubmissions(status);

            return ApiResponse.success("查询实验报告成功", submissions);

        } catch (IllegalArgumentException e) {
            return ApiResponse.error(e.getMessage());
        } catch (Exception e) {
            return ApiResponse.error("查询实验报告失败");
        }
    }

    /**
     * 查询报告详情
     *
     * GET /api/admin/submissions/{id}
     */
    @GetMapping("/{id}")
    public ApiResponse<ExperimentSubmit> getSubmission(
            @PathVariable Long id,
            HttpServletRequest request) {

        try {
            getAdmin(request);

            ExperimentSubmit submission =
                    adminSubmissionService.getSubmission(id);

            return ApiResponse.success("查询报告详情成功", submission);

        } catch (IllegalArgumentException e) {
            return ApiResponse.error(e.getMessage());
        } catch (Exception e) {
            return ApiResponse.error("查询报告详情失败");
        }
    }

    /**
     * 审核实验报告
     *
     * PUT /api/admin/submissions/{id}/review
     */
    @PutMapping("/{id}/review")
    public ApiResponse<ExperimentSubmit> review(
            @PathVariable Long id,
            @Valid @RequestBody ReviewRequest reviewRequest,
            HttpServletRequest request) {

        try {
            SysUser admin = getAdmin(request);

            ExperimentSubmit submission =
                    adminSubmissionService.review(
                            admin.getId(),
                            id,
                            reviewRequest.getReviewStatus(),
                            reviewRequest.getScore(),
                            reviewRequest.getReviewComment()
                    );

            return ApiResponse.success("实验报告审核成功", submission);

        } catch (IllegalArgumentException e) {
            return ApiResponse.error(e.getMessage());
        } catch (Exception e) {
            return ApiResponse.error("实验报告审核失败");
        }
    }

    /**
     * 获取当前管理员
     */
    private SysUser getAdmin(HttpServletRequest request) {

        HttpSession session = request.getSession(false);

        if (session == null) {
            throw new IllegalArgumentException("请先登录");
        }

        Object userObject = session.getAttribute("loginUser");

        if (!(userObject instanceof SysUser)) {
            throw new IllegalArgumentException("登录状态无效，请重新登录");
        }

        SysUser user = (SysUser) userObject;

        if (user.getId() == null || !"ADMIN".equals(user.getRole())) {
            throw new IllegalArgumentException("只有管理员可以执行此操作");
        }

        return user;
    }
}