
package com.websecurity.platform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.websecurity.platform.entity.LabSession;
import com.websecurity.platform.mapper.LabSessionMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class LabPortAllocator {

    private final LabSessionMapper labSessionMapper;
    private final DockerComposeManager dockerComposeManager;

    @Value("${app.docker.port-start:4292}")
    private int portStart;

    @Value("${app.docker.port-end:4399}")
    private int portEnd;

    public LabPortAllocator(
            LabSessionMapper labSessionMapper,
            DockerComposeManager dockerComposeManager) {

        this.labSessionMapper = labSessionMapper;
        this.dockerComposeManager = dockerComposeManager;
    }

    /**
     * 判断端口是否已经分配给其他实验任务。
     */
    public boolean isReservedByOtherTask(
            int port,
            Long taskId) {

        Long count = labSessionMapper.selectCount(
                new LambdaQueryWrapper<LabSession>()
                        .eq(LabSession::getHostPort, port)
                        .ne(LabSession::getTaskId, taskId)
        );

        return count != null && count > 0;
    }

    /**
     * 判断指定任务能否继续使用自己的原端口。
     */
    public boolean canReusePort(
            int port,
            Long taskId) {

        if (port < portStart || port > portEnd) {
            return false;
        }

        if (!dockerComposeManager.isPortAvailable(port)) {
            return false;
        }

        return !isReservedByOtherTask(port, taskId);
    }

    /**
     * 为指定任务分配一个新端口。
     */
    public synchronized int allocate(Long taskId) {

        if (taskId == null || taskId <= 0) {
            throw new IllegalArgumentException(
                    "实验任务ID不合法"
            );
        }

        if (portStart < 1024
                || portEnd > 65535
                || portStart > portEnd) {

            throw new IllegalStateException(
                    "Docker端口范围配置不正确"
            );
        }

        for (int port = portStart; port <= portEnd; port++) {

            // 检查操作系统端口
            if (!dockerComposeManager.isPortAvailable(port)) {
                continue;
            }

            // 检查数据库中是否已分配给其他任务
            if (isReservedByOtherTask(port, taskId)) {
                continue;
            }

            return port;
        }

        throw new IllegalStateException(
                "没有可分配的DVWA端口，请检查端口范围"
        );
    }
}
