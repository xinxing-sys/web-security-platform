
package com.websecurity.platform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.websecurity.platform.entity.Experiment;
import com.websecurity.platform.entity.ExperimentTask;
import com.websecurity.platform.entity.LabSession;
import com.websecurity.platform.mapper.ExperimentMapper;
import com.websecurity.platform.mapper.ExperimentTaskMapper;
import com.websecurity.platform.mapper.LabSessionMapper;
import com.websecurity.platform.service.LabSessionService;
import org.springframework.stereotype.Service;

@Service
public class LabSessionServiceImpl implements LabSessionService {

    private final LabSessionMapper labSessionMapper;
    private final ExperimentTaskMapper taskMapper;
    private final ExperimentMapper experimentMapper;

    public LabSessionServiceImpl(
            LabSessionMapper labSessionMapper,
            ExperimentTaskMapper taskMapper,
            ExperimentMapper experimentMapper) {

        this.labSessionMapper = labSessionMapper;
        this.taskMapper = taskMapper;
        this.experimentMapper = experimentMapper;
    }

    /**
     * 查询当前学生指定任务对应的靶场实例。
     */
    @Override
    public LabSession getMySession(Long userId, Long taskId) {

        validateIds(userId, taskId);

        // 首先验证任务属于当前学生
        ExperimentTask task = findMyTask(userId, taskId);

        if (task == null) {
            throw new IllegalArgumentException(
                    "任务不存在或无权查看"
            );
        }

        // 根据任务ID查询靶场实例
        return labSessionMapper.selectOne(
                new LambdaQueryWrapper<LabSession>()
                        .eq(LabSession::getTaskId, taskId)
                        .eq(LabSession::getUserId, userId)
        );
    }

    /**
     * 启动靶场前校验任务。
     */
    @Override
    public ExperimentTask validateTaskForLab(
            Long userId,
            Long taskId) {

        validateIds(userId, taskId);

        // 1. 查询当前学生自己的任务
        ExperimentTask task = findMyTask(userId, taskId);

        if (task == null) {
            throw new IllegalArgumentException(
                    "任务不存在或无权操作"
            );
        }

        // 2. 必须先通过平台开始实验
        if (!"IN_PROGRESS".equals(task.getStatus())) {
            throw new IllegalArgumentException(
                    "请先开始实验，再启动靶场"
            );
        }

        // 3. 查询对应实验
        Experiment experiment =
                experimentMapper.selectById(task.getExperimentId());

        if (experiment == null) {
            throw new IllegalArgumentException(
                    "关联的实验资源不存在"
            );
        }

        // 4. 实验必须处于启用状态
        if (experiment.getStatus() == null
                || experiment.getStatus() != 1) {
            throw new IllegalArgumentException(
                    "该实验资源已停用"
            );
        }

        // 5. 只允许启动DVWA类型的实验
        if (!"DVWA".equalsIgnoreCase(
                experiment.getPlatformType() == null
                        ? ""
                        : experiment.getPlatformType().trim())) {

            throw new IllegalArgumentException(
                    "当前实验尚未配置为DVWA靶场"
            );
        }

        return task;
    }

    /**
     * 查询属于当前学生的任务。
     */
    private ExperimentTask findMyTask(
            Long userId,
            Long taskId) {

        return taskMapper.selectOne(
                new LambdaQueryWrapper<ExperimentTask>()
                        .eq(ExperimentTask::getId, taskId)
                        .eq(ExperimentTask::getUserId, userId)
        );
    }

    /**
     * 检查用户ID和任务ID。
     */
    private void validateIds(Long userId, Long taskId) {

        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException(
                    "用户信息无效"
            );
        }

        if (taskId == null || taskId <= 0) {
            throw new IllegalArgumentException(
                    "任务ID不合法"
            );
        }
    }
}
