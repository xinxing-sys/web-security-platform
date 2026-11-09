package com.websecurity.platform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.websecurity.platform.entity.ExperimentSubmit;
import com.websecurity.platform.entity.ExperimentTask;
import com.websecurity.platform.mapper.ExperimentSubmitMapper;
import com.websecurity.platform.mapper.ExperimentTaskMapper;
import com.websecurity.platform.service.AdminSubmissionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class AdminSubmissionServiceImpl implements AdminSubmissionService {

    private final ExperimentSubmitMapper submitMapper;
    private final ExperimentTaskMapper taskMapper;

    public AdminSubmissionServiceImpl(
            ExperimentSubmitMapper submitMapper,
            ExperimentTaskMapper taskMapper) {
        this.submitMapper = submitMapper;
        this.taskMapper = taskMapper;
    }

    /**
     * 查询实验报告列表
     */
    @Override
    public List<ExperimentSubmit> querySubmissions(String reviewStatus) {

        LambdaQueryWrapper<ExperimentSubmit> wrapper =
                new LambdaQueryWrapper<>();

        if (reviewStatus != null && !reviewStatus.trim().isEmpty()) {

            String status = reviewStatus.trim().toUpperCase();

            if (!"PENDING".equals(status)
                    && !"APPROVED".equals(status)
                    && !"REJECTED".equals(status)) {
                throw new IllegalArgumentException("审核状态不正确");
            }

            wrapper.eq(ExperimentSubmit::getReviewStatus, status);
        }

        wrapper.orderByDesc(ExperimentSubmit::getSubmittedAt)
                .orderByDesc(ExperimentSubmit::getId);

        return submitMapper.selectList(wrapper);
    }

    /**
     * 查询报告详情
     */
    @Override
    public ExperimentSubmit getSubmission(Long submissionId) {

        if (submissionId == null) {
            throw new IllegalArgumentException("报告ID不能为空");
        }

        ExperimentSubmit submission =
                submitMapper.selectById(submissionId);

        if (submission == null) {
            throw new IllegalArgumentException("实验报告不存在");
        }

        return submission;
    }

    /**
     * 审核实验报告
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ExperimentSubmit review(
            Long reviewerId,
            Long submissionId,
            String reviewStatus,
            BigDecimal score,
            String reviewComment) {

        if (reviewerId == null || submissionId == null) {
            throw new IllegalArgumentException("审核参数不完整");
        }

        if (score == null
                || score.compareTo(BigDecimal.ZERO) < 0
                || score.compareTo(new BigDecimal("100")) > 0) {
            throw new IllegalArgumentException("成绩必须在0到100之间");
        }

        if (reviewStatus == null) {
            throw new IllegalArgumentException("审核状态不能为空");
        }

        String status = reviewStatus.trim().toUpperCase();

        if (!"APPROVED".equals(status)
                && !"REJECTED".equals(status)) {
            throw new IllegalArgumentException(
                    "审核状态只能是 APPROVED 或 REJECTED");
        }

        // 查询报告
        ExperimentSubmit submission =
                submitMapper.selectById(submissionId);

        if (submission == null) {
            throw new IllegalArgumentException("实验报告不存在");
        }

        // 只能审核待审核报告
        if (!"PENDING".equals(submission.getReviewStatus())) {
            throw new IllegalArgumentException(
                    "该报告已经审核，不能重复审核");
        }

        // 查询对应实验任务
        ExperimentTask task =
                taskMapper.selectById(submission.getTaskId());

        if (task == null) {
            throw new IllegalArgumentException("对应的实验任务不存在");
        }

        // 检查报告与任务归属关系
        if (!submission.getUserId().equals(task.getUserId())) {
            throw new IllegalArgumentException("报告与实验任务信息不匹配");
        }

        if (!"SUBMITTED".equals(task.getStatus())) {
            throw new IllegalArgumentException(
                    "当前实验任务状态不允许审核");
        }

        LocalDateTime now = LocalDateTime.now();

        // 更新报告审核信息
        int submitUpdateResult = submitMapper.update(
                null,
                new LambdaUpdateWrapper<ExperimentSubmit>()
                        .eq(ExperimentSubmit::getId, submissionId)
                        .eq(ExperimentSubmit::getReviewStatus, "PENDING")
                        .set(ExperimentSubmit::getReviewStatus, status)
                        .set(ExperimentSubmit::getScore, score)
                        .set(ExperimentSubmit::getReviewerId, reviewerId)
                        .set(ExperimentSubmit::getReviewComment, reviewComment)
                        .set(ExperimentSubmit::getReviewedAt, now)
        );

        if (submitUpdateResult != 1) {
            throw new IllegalArgumentException(
                    "报告审核失败，可能已经被其他管理员处理");
        }

        // 更新任务状态
        LambdaUpdateWrapper<ExperimentTask> taskWrapper =
                new LambdaUpdateWrapper<>();

        taskWrapper
                .eq(ExperimentTask::getId, task.getId())
                .eq(ExperimentTask::getUserId, task.getUserId())
                .eq(ExperimentTask::getStatus, "SUBMITTED");

        if ("APPROVED".equals(status)) {

            // 审核通过：任务完成
            taskWrapper
                    .set(ExperimentTask::getStatus, "COMPLETED")
                    .set(ExperimentTask::getCompletedAt, now);

        } else {

            // 审核驳回：任务恢复进行中，允许学生重新提交
            taskWrapper
                    .set(ExperimentTask::getStatus, "IN_PROGRESS")
                    .set(ExperimentTask::getCompletedAt, null);
        }

        int taskUpdateResult = taskMapper.update(null, taskWrapper);

        if (taskUpdateResult != 1) {
            throw new IllegalArgumentException(
                    "实验任务状态更新失败，审核操作已回滚");
        }

        // 返回更新后的报告
        return submitMapper.selectById(submissionId);
    }
}