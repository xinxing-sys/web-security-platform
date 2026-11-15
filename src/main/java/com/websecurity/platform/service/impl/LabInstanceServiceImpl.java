
package com.websecurity.platform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.websecurity.platform.entity.LabSession;
import com.websecurity.platform.mapper.LabSessionMapper;
import com.websecurity.platform.service.LabInstanceService;
import com.websecurity.platform.service.LabSessionService;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.time.LocalDateTime;

@Service
public class LabInstanceServiceImpl implements LabInstanceService {

    private static final String STATUS_STARTING = "STARTING";
    private static final String STATUS_RUNNING = "RUNNING";
    private static final String STATUS_FAILED = "FAILED";

    private final LabSessionMapper labSessionMapper;
    private final LabSessionService labSessionService;
    private final DockerComposeManager dockerComposeManager;

    private final LabPortAllocator labPortAllocator;

    public LabInstanceServiceImpl(
            LabSessionMapper labSessionMapper,
            LabSessionService labSessionService,
            DockerComposeManager dockerComposeManager,
            LabPortAllocator labPortAllocator) {

        this.labSessionMapper = labSessionMapper;
        this.labSessionService = labSessionService;
        this.dockerComposeManager = dockerComposeManager;
        this.labPortAllocator = labPortAllocator;
    }

    /**
     * 启动或复用靶场实例。
     *
     * 一个实验任务只对应一个DVWA实例。
     */
    @Override
    public synchronized LabSession startOrReuse(
            Long userId,
            Long taskId) {

        // 1. 验证任务归属、任务状态和实验类型
        labSessionService.validateTaskForLab(userId, taskId);

        // 2. 根据任务ID生成固定的Docker项目名称
        String projectName = "lab_task_" + taskId;

        // 3. 查询当前任务是否已经存在实例记录
        LabSession session = labSessionMapper.selectOne(
                new LambdaQueryWrapper<LabSession>()
                        .eq(LabSession::getTaskId, taskId)
                        .eq(LabSession::getUserId, userId)
                        .last("LIMIT 1")
        );

        // 4. 已有运行中的实例，先检查容器是否真实运行
        if (session != null
                && STATUS_RUNNING.equals(session.getStatus())) {

            boolean webRunning =
                    dockerComposeManager.isContainerRunning(
                            session.getWebContainerId()
                    );

            boolean dbRunning =
                    dockerComposeManager.isContainerRunning(
                            session.getDbContainerId()
                    );

            if (webRunning && dbRunning) {
                return session;
            }
        }

// 5. 分配或复用端口
        int port;

        if (session != null
                && session.getHostPort() != null
                && labPortAllocator.canReusePort(
                session.getHostPort(),
                taskId
        )) {

            // 当前任务继续使用自己的原端口
            port = session.getHostPort();

        } else {

            // 为当前任务分配一个没有被其他任务占用的新端口
            port = labPortAllocator.allocate(taskId);
        }

        // 6. 创建或更新实例数据库记录
        if (session == null) {

            session = new LabSession();

            session.setUserId(userId);
            session.setTaskId(taskId);
            session.setLabType("DVWA");
            session.setInstanceKey(projectName);
            session.setHostPort(port);
            session.setStatus(STATUS_STARTING);

            labSessionMapper.insert(session);

        } else {

            session.setInstanceKey(projectName);
            session.setHostPort(port);
            session.setStatus(STATUS_STARTING);
            session.setLastError(null);

            labSessionMapper.updateById(session);
        }

        Path envFile = null;

        try {

            // 7. 为当前任务生成独立环境变量文件
            envFile = dockerComposeManager.prepareInstance(
                    projectName,
                    port
            );

            // 8. 启动Docker Compose项目
            dockerComposeManager.start(
                    projectName,
                    envFile
            );

            // 9. 获取Web容器ID和数据库容器ID
            String webContainerId =
                    dockerComposeManager.getContainerId(
                            projectName,
                            envFile,
                            "dvwa"
                    );

            String dbContainerId =
                    dockerComposeManager.getContainerId(
                            projectName,
                            envFile,
                            "db"
                    );

            if (webContainerId == null
                    || dbContainerId == null) {

                throw new IllegalStateException(
                        "Docker容器创建失败，未能获取完整容器ID"
                );
            }

            // 10. 等待Web容器启动
            dockerComposeManager.waitForWebContainer(
                    webContainerId
            );

            // 11. 检查数据库容器是否运行
            if (!dockerComposeManager.isContainerRunning(
                    dbContainerId
            )) {
                throw new IllegalStateException(
                        "DVWA数据库容器未正常运行"
                );
            }

            // 12. 等待DVWA网页可以正常响应
            dockerComposeManager.waitForHttp(
                    "http://127.0.0.1:" + port + "/setup.php"
            );

            // 12. 更新实例运行状态
            session.setWebContainerId(webContainerId);
            session.setDbContainerId(dbContainerId);
            session.setHostPort(port);
            session.setAccessUrl(
                    "http://localhost:" + port
            );
            session.setStatus(STATUS_RUNNING);
            session.setStartedAt(LocalDateTime.now());
            session.setStoppedAt(null);
            session.setLastError(null);

            labSessionMapper.updateById(session);

            return session;


        } catch (Exception e) {

            // 13. 启动失败，尝试清理容器和网络
            String errorMessage = e.getMessage();

            if (envFile != null) {
                try {
                    dockerComposeManager.stop(
                            projectName,
                            envFile
                    );
                } catch (Exception cleanupException) {
                    errorMessage = errorMessage
                            + "；容器清理失败："
                            + cleanupException.getMessage();
                }
            }

            // 14. 记录失败状态
            session.setStatus(STATUS_FAILED);
            session.setLastError(errorMessage);

            labSessionMapper.updateById(session);

            throw new IllegalStateException(
                    "DVWA靶场实例启动失败：" + errorMessage,
                    e
            );
        }

    }
}
