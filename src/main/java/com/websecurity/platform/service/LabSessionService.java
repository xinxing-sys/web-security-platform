
package com.websecurity.platform.service;

import com.websecurity.platform.entity.ExperimentTask;
import com.websecurity.platform.entity.LabSession;

public interface LabSessionService {

    /**
     * 查询当前学生指定任务对应的靶场实例。
     * 如果尚未分配实例，返回 null。
     */
    LabSession getMySession(Long userId, Long taskId);

    /**
     * 启动靶场前校验任务是否符合要求。
     * 返回校验通过的实验任务。
     */
    ExperimentTask validateTaskForLab(Long userId, Long taskId);
}
