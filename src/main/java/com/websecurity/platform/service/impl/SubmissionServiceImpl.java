package com.websecurity.platform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.websecurity.platform.entity.ExperimentSubmit;
import com.websecurity.platform.entity.ExperimentTask;
import com.websecurity.platform.mapper.ExperimentSubmitMapper;
import com.websecurity.platform.mapper.ExperimentTaskMapper;
import com.websecurity.platform.service.SubmissionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class SubmissionServiceImpl implements SubmissionService {

    private final ExperimentSubmitMapper submitMapper;
    private final ExperimentTaskMapper taskMapper;

    public SubmissionServiceImpl(
            ExperimentSubmitMapper submitMapper,
            ExperimentTaskMapper taskMapper) {
        this.submitMapper = submitMapper;
        this.taskMapper = taskMapper;
    }

    /**
     * 学生提交实验报告
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ExperimentSubmit submit(
            Long userId,
            Long taskId,
            String content) {

        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("用户信息无效");
        }

        if (taskId == null || taskId <= 0) {
            throw new IllegalArgumentException("任务ID不合法");
        }

        if (content == null || content.trim().isEmpty()) {
            throw new IllegalArgumentException("实验报告内容不能为空");
        }

        // 只能查询当前学生自己的任务
        ExperimentTask task = taskMapper.selectOne(
                new LambdaQueryWrapper<ExperimentTask>()
                        .eq(ExperimentTask::getId, taskId)
                        .eq(ExperimentTask::getUserId, userId)
        );

        if (task == null) {
            throw new IllegalArgumentException(
                    "实验任务不存在或无权操作"
            );
        }

        // 只有正在进行中的任务允许提交
        if (!"IN_PROGRESS".equals(task.getStatus())) {
            throw new IllegalArgumentException(
                    "当前任务状态不允许提交报告"
            );
        }

        // 查询该任务最近一次提交记录
        ExperimentSubmit lastSubmission = submitMapper.selectOne(
                new LambdaQueryWrapper<ExperimentSubmit>()
                        .eq(ExperimentSubmit::getTaskId, taskId)
                        .eq(ExperimentSubmit::getUserId, userId)
                        .orderByDesc(ExperimentSubmit::getSubmittedAt)
                        .orderByDesc(ExperimentSubmit::getId)
                        .last("LIMIT 1")
        );

        // 第一次提交允许直接提交
        // 后续提交必须建立在上一次被驳回的基础上
        if (lastSubmission != null) {

            if ("PENDING".equals(lastSubmission.getReviewStatus())) {
                throw new IllegalArgumentException(
                        "上一份报告正在审核，请等待审核结果"
                );
            }

            if ("APPROVED".equals(lastSubmission.getReviewStatus())) {
                throw new IllegalArgumentException(
                        "该实验报告已审核通过，不能重复提交"
                );
            }

            if (!"REJECTED".equals(lastSubmission.getReviewStatus())) {
                throw new IllegalArgumentException(
                        "当前报告状态不允许重新提交"
                );
            }
        }

        // 创建新的报告记录，不覆盖历史报告
        ExperimentSubmit submission = new ExperimentSubmit();

        submission.setTaskId(taskId);
        submission.setUserId(userId);
        submission.setContent(content.trim());
        submission.setReviewStatus("PENDING");
        submission.setSubmittedAt(LocalDateTime.now());

        // 附件功能后续阶段实现
        submission.setAttachmentPath(null);

        int insertResult = submitMapper.insert(submission);

        if (insertResult != 1) {
            throw new IllegalArgumentException("实验报告提交失败");
        }

        // 更新任务状态
        int updateResult = taskMapper.update(
                null,
                new LambdaUpdateWrapper<ExperimentTask>()
                        .eq(ExperimentTask::getId, taskId)
                        .eq(ExperimentTask::getUserId, userId)
                        .eq(ExperimentTask::getStatus, "IN_PROGRESS")
                        .set(ExperimentTask::getStatus, "SUBMITTED")
        );

        if (updateResult != 1) {
            throw new IllegalArgumentException(
                    "任务状态更新失败，请重新提交"
            );
        }

        return submission;
    }

    /**
     * 查询当前学生某个任务的全部报告历史
     */
    @Override
    public List<ExperimentSubmit> queryMySubmissions(
            Long userId,
            Long taskId) {

        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("用户信息无效");
        }

        if (taskId == null || taskId <= 0) {
            throw new IllegalArgumentException("任务ID不合法");
        }

        // 先检查任务归属
        ExperimentTask task = taskMapper.selectOne(
                new LambdaQueryWrapper<ExperimentTask>()
                        .eq(ExperimentTask::getId, taskId)
                        .eq(ExperimentTask::getUserId, userId)
        );

        if (task == null) {
            throw new IllegalArgumentException(
                    "实验任务不存在或无权查看"
            );
        }

        return submitMapper.selectList(
                new LambdaQueryWrapper<ExperimentSubmit>()
                        .eq(ExperimentSubmit::getTaskId, taskId)
                        .eq(ExperimentSubmit::getUserId, userId)
                        .orderByDesc(ExperimentSubmit::getSubmittedAt)
                        .orderByDesc(ExperimentSubmit::getId)
        );
    }

    /**
     * 查询当前学生自己的报告详情
     */
    @Override
    public ExperimentSubmit getMySubmission(
            Long userId,
            Long submissionId) {

        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("用户信息无效");
        }

        if (submissionId == null || submissionId <= 0) {
            throw new IllegalArgumentException("报告ID不合法");
        }

        ExperimentSubmit submission = submitMapper.selectOne(
                new LambdaQueryWrapper<ExperimentSubmit>()
                        .eq(ExperimentSubmit::getId, submissionId)
                        .eq(ExperimentSubmit::getUserId, userId)
        );

        if (submission == null) {
            throw new IllegalArgumentException(
                    "报告不存在或无权查看"
            );
        }

        return submission;
    }
}