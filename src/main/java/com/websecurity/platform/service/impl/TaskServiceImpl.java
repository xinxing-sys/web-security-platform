package com.websecurity.platform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.websecurity.platform.entity.Experiment;
import com.websecurity.platform.entity.ExperimentTask;
import com.websecurity.platform.mapper.ExperimentMapper;
import com.websecurity.platform.mapper.ExperimentTaskMapper;
import com.websecurity.platform.service.TaskService;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class TaskServiceImpl implements TaskService {

    private final ExperimentTaskMapper taskMapper;
    private final ExperimentMapper experimentMapper;

    public TaskServiceImpl(
            ExperimentTaskMapper taskMapper,
            ExperimentMapper experimentMapper) {
        this.taskMapper = taskMapper;
        this.experimentMapper = experimentMapper;
    }

    /**
     * 学生领取实验
     */
    @Override
    @Transactional
    public ExperimentTask claim(Long userId, Long experimentId) {

        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("用户信息无效");
        }

        if (experimentId == null || experimentId <= 0) {
            throw new IllegalArgumentException("实验ID不合法");
        }

        Experiment experiment = experimentMapper.selectById(experimentId);

        if (experiment == null) {
            throw new IllegalArgumentException("实验资源不存在");
        }

        if (experiment.getStatus() == null
                || experiment.getStatus() != 1) {
            throw new IllegalArgumentException("该实验当前不可领取");
        }

        // 检查是否已经领取
        Long count = taskMapper.selectCount(
                new LambdaQueryWrapper<ExperimentTask>()
                        .eq(ExperimentTask::getUserId, userId)
                        .eq(ExperimentTask::getExperimentId, experimentId)
        );

        if (count != null && count > 0) {
            throw new IllegalArgumentException("你已经领取过该实验，请在我的任务中查看");
        }

        ExperimentTask task = new ExperimentTask();
        task.setUserId(userId);
        task.setExperimentId(experimentId);
        task.setStatus("CLAIMED");

        try {
            taskMapper.insert(task);
        } catch (DuplicateKeyException e) {
            // 防止并发请求绕过前面的重复检查
            throw new IllegalArgumentException("你已经领取过该实验");
        }

        return task;
    }

    /**
     * 分页查询当前学生的任务
     */
    @Override
    public IPage<ExperimentTask> queryMyTasks(
            Long userId,
            Integer pageNum,
            Integer pageSize,
            String status) {

        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("用户信息无效");
        }

        int current = (pageNum == null || pageNum < 1)
                ? 1 : pageNum;

        int size = (pageSize == null || pageSize < 1)
                ? 10 : Math.min(pageSize, 100);

        if (status != null && !status.trim().isEmpty()
                && !isValidTaskStatus(status.trim().toUpperCase())) {
            throw new IllegalArgumentException("任务状态不合法");
        }

        LambdaQueryWrapper<ExperimentTask> wrapper =
                new LambdaQueryWrapper<>();

        wrapper.eq(ExperimentTask::getUserId, userId);

        wrapper.eq(
                status != null && !status.trim().isEmpty(),
                ExperimentTask::getStatus,
                status == null ? null : status.trim().toUpperCase()
        );

        wrapper.orderByDesc(ExperimentTask::getCreatedAt);

        return taskMapper.selectPage(
                new Page<>(current, size),
                wrapper
        );
    }

    /**
     * 查询当前学生自己的任务详情
     */
    @Override
    public ExperimentTask getMyTask(Long userId, Long taskId) {

        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("用户信息无效");
        }

        if (taskId == null || taskId <= 0) {
            throw new IllegalArgumentException("任务ID不合法");
        }

        ExperimentTask task = taskMapper.selectOne(
                new LambdaQueryWrapper<ExperimentTask>()
                        .eq(ExperimentTask::getId, taskId)
                        .eq(ExperimentTask::getUserId, userId)
        );

        if (task == null) {
            throw new IllegalArgumentException("任务不存在或无权查看");
        }

        return task;
    }

    /**
     * 开始实验
     */
    @Override
    @Transactional
    public ExperimentTask start(Long userId, Long taskId) {

        ExperimentTask task = getMyTask(userId, taskId);

        if ("IN_PROGRESS".equals(task.getStatus())) {
            return task;
        }

        if (!"CLAIMED".equals(task.getStatus())) {
            throw new IllegalArgumentException(
                    "当前任务状态不允许开始实验"
            );
        }

        Experiment experiment =
                experimentMapper.selectById(task.getExperimentId());

        if (experiment == null || experiment.getStatus() == null
                || experiment.getStatus() != 1) {
            throw new IllegalArgumentException(
                    "实验资源已停用，暂时无法开始"
            );
        }

        task.setStatus("IN_PROGRESS");
        task.setStartedAt(LocalDateTime.now());

        taskMapper.updateById(task);

        return task;
    }

    /**
     * 校验任务状态
     */
    private boolean isValidTaskStatus(String status) {
        return "CLAIMED".equals(status)
                || "IN_PROGRESS".equals(status)
                || "SUBMITTED".equals(status)
                || "COMPLETED".equals(status);
    }
}