
package com.websecurity.platform.service;

import com.websecurity.platform.entity.LabSession;

public interface LabInstanceService {

    /**
     * 启动或复用当前学生的实验靶场实例。
     */
    LabSession startOrReuse(Long userId, Long taskId);
}
