package com.websecurity.platform.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.websecurity.platform.entity.ExperimentTask;

public interface TaskService {

    ExperimentTask claim(Long userId, Long experimentId);

    IPage<ExperimentTask> queryMyTasks(
            Long userId,
            Integer pageNum,
            Integer pageSize,
            String status
    );

    ExperimentTask getMyTask(Long userId, Long taskId);

    ExperimentTask start(Long userId, Long taskId);
}