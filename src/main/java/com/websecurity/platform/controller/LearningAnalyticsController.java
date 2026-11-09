
package com.websecurity.platform.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.websecurity.platform.common.ApiResponse;
import com.websecurity.platform.entity.Experiment;
import com.websecurity.platform.entity.ExperimentSubmit;
import com.websecurity.platform.entity.ExperimentTask;
import com.websecurity.platform.entity.SysUser;
import com.websecurity.platform.mapper.ExperimentMapper;
import com.websecurity.platform.mapper.ExperimentSubmitMapper;
import com.websecurity.platform.mapper.ExperimentTaskMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
public class LearningAnalyticsController {

    private final ExperimentTaskMapper taskMapper;
    private final ExperimentSubmitMapper submitMapper;
    private final ExperimentMapper experimentMapper;

    public LearningAnalyticsController(
            ExperimentTaskMapper taskMapper,
            ExperimentSubmitMapper submitMapper,
            ExperimentMapper experimentMapper) {
        this.taskMapper = taskMapper;
        this.submitMapper = submitMapper;
        this.experimentMapper = experimentMapper;
    }

    /**
     * 获取当前学生的学习分析数据
     * GET /api/student/learning/overview
     */
    @GetMapping("/api/student/learning/overview")
    public ApiResponse<Map<String, Object>> getOverview(
            HttpServletRequest request) {

        try {
            SysUser user = getStudent(request);
            Long userId = user.getId();

            // 1. 查询当前学生的全部实验任务
            List<ExperimentTask> tasks = taskMapper.selectList(
                    new LambdaQueryWrapper<ExperimentTask>()
                            .eq(ExperimentTask::getUserId, userId)
                            .orderByDesc(ExperimentTask::getCreatedAt)
            );

            int totalTasks = tasks.size();

            long completedTasks = tasks.stream()
                    .filter(task -> "COMPLETED".equals(task.getStatus()))
                    .count();

            long claimedTasks = tasks.stream()
                    .filter(task -> "CLAIMED".equals(task.getStatus()))
                    .count();

            long inProgressTasks = tasks.stream()
                    .filter(task -> "IN_PROGRESS".equals(task.getStatus()))
                    .count();

            long pendingTasks = tasks.stream()
                    .filter(task -> "SUBMITTED".equals(task.getStatus()))
                    .count();

            // 2. 计算完成率
            BigDecimal completionRate = BigDecimal.ZERO;

            if (totalTasks > 0) {
                completionRate = BigDecimal.valueOf(completedTasks)
                        .multiply(BigDecimal.valueOf(100))
                        .divide(
                                BigDecimal.valueOf(totalTasks),
                                1,
                                RoundingMode.HALF_UP
                        );
            }

            // 3. 查询实验名称
            Set<Long> experimentIds = tasks.stream()
                    .map(ExperimentTask::getExperimentId)
                    .filter(id -> id != null)
                    .collect(Collectors.toSet());

            Map<Long, Experiment> experimentMap = new HashMap<>();

            if (!experimentIds.isEmpty()) {
                List<Experiment> experiments =
                        experimentMapper.selectBatchIds(experimentIds);

                for (Experiment experiment : experiments) {
                    experimentMap.put(experiment.getId(), experiment);
                }
            }

            // 4. 查询当前学生的全部报告
            List<ExperimentSubmit> submissions =
                    submitMapper.selectList(
                            new LambdaQueryWrapper<ExperimentSubmit>()
                                    .eq(ExperimentSubmit::getUserId, userId)
                                    .orderByDesc(ExperimentSubmit::getSubmittedAt)
                                    .orderByDesc(ExperimentSubmit::getId)
                    );

            // 每个任务只保留最近一次报告
            Map<Long, ExperimentSubmit> latestSubmissionMap =
                    new HashMap<>();

            for (ExperimentSubmit submission : submissions) {
                latestSubmissionMap.putIfAbsent(
                        submission.getTaskId(),
                        submission
                );
            }

            // 5. 计算成绩：只统计每个任务最近一次已审核且有成绩的报告
            List<BigDecimal> scores = latestSubmissionMap.values()
                    .stream()
                    .filter(item -> item.getScore() != null)
                    .filter(item ->
                            "APPROVED".equals(item.getReviewStatus())
                                    || "REJECTED".equals(item.getReviewStatus())
                    )
                    .map(ExperimentSubmit::getScore)
                    .toList();

            BigDecimal averageScore = BigDecimal.ZERO;

            if (!scores.isEmpty()) {
                BigDecimal totalScore = scores.stream()
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                averageScore = totalScore.divide(
                        BigDecimal.valueOf(scores.size()),
                        2,
                        RoundingMode.HALF_UP
                );
            }

            // 6. 生成最近学习记录
            List<Map<String, Object>> records = new ArrayList<>();

            for (ExperimentTask task : tasks.stream()
                    .limit(10)
                    .toList()) {

                Map<String, Object> record = new LinkedHashMap<>();

                Experiment experiment =
                        experimentMap.get(task.getExperimentId());

                ExperimentSubmit submission =
                        latestSubmissionMap.get(task.getId());

                record.put("taskId", task.getId());
                record.put("experimentId", task.getExperimentId());
                record.put(
                        "experimentName",
                        experiment == null
                                ? "实验资源已不存在"
                                : experiment.getName()
                );
                record.put("difficulty",
                        experiment == null
                                ? "-"
                                : experiment.getDifficulty()
                );
                record.put("status", task.getStatus());
                record.put("createdAt", task.getCreatedAt());
                record.put(
                        "score",
                        submission == null ? null : submission.getScore()
                );
                record.put(
                        "reviewStatus",
                        submission == null
                                ? null
                                : submission.getReviewStatus()
                );

                records.add(record);
            }

            // 7. 返回统计结果
            Map<String, Object> result = new LinkedHashMap<>();

            result.put("totalTasks", totalTasks);
            result.put("completedTasks", completedTasks);
            result.put("claimedTasks", claimedTasks);
            result.put("inProgressTasks", inProgressTasks);
            result.put("pendingTasks", pendingTasks);
            result.put("completionRate", completionRate);
            result.put("averageScore", averageScore);
            result.put("scoredTaskCount", scores.size());
            result.put("records", records);

            return ApiResponse.success(
                    "获取学习分析数据成功",
                    result
            );

        } catch (IllegalArgumentException e) {
            return ApiResponse.error(e.getMessage());

        } catch (Exception e) {
            e.printStackTrace();
            return ApiResponse.error("获取学习分析数据失败");
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
                    "只有学生账号可以查看学习分析"
            );
        }

        return user;
    }
}
