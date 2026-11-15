
package com.websecurity.platform.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.websecurity.platform.common.ApiResponse;
import com.websecurity.platform.entity.LabSession;
import com.websecurity.platform.entity.SysUser;
import com.websecurity.platform.mapper.LabSessionMapper;
import com.websecurity.platform.service.impl.DockerComposeManager;
import com.websecurity.platform.vo.AdminLabSessionVO;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/admin/labs")
public class AdminLabController {

    private final LabSessionMapper labSessionMapper;
    private final DockerComposeManager dockerComposeManager;

    public AdminLabController(
            LabSessionMapper labSessionMapper,
            DockerComposeManager dockerComposeManager) {

        this.labSessionMapper = labSessionMapper;
        this.dockerComposeManager = dockerComposeManager;
    }

    /**
     * 查询所有靶场实例
     */
    @GetMapping
    public ApiResponse<List<AdminLabSessionVO>> list(
            HttpSession session) {

        try {
            requireAdmin(session);

            List<LabSession> sessions = labSessionMapper.selectList(
                    new LambdaQueryWrapper<LabSession>()
                            .orderByDesc(LabSession::getCreatedAt)
            );

            List<AdminLabSessionVO> result = new ArrayList<>();

            for (LabSession item : sessions) {

                boolean webRunning =
                        dockerComposeManager.isContainerRunning(
                                item.getWebContainerId()
                        );

                boolean dbRunning =
                        dockerComposeManager.isContainerRunning(
                                item.getDbContainerId()
                        );

                result.add(AdminLabSessionVO.fromEntity(
                        item,
                        webRunning,
                        dbRunning
                ));
            }

            return ApiResponse.success(
                    "靶场实例列表查询成功",
                    result
            );

        } catch (IllegalArgumentException e) {
            return ApiResponse.error(e.getMessage());

        } catch (Exception e) {
            return ApiResponse.error(
                    "查询靶场实例列表失败：" + e.getMessage()
            );
        }
    }

    /**
     * 停止指定靶场实例。
     *
     * Docker Compose down 会删除容器和网络，
     * 但不删除数据库数据卷。
     */
    @PostMapping("/{id}/stop")
    public ApiResponse<Void> stop(
            @PathVariable Long id,
            HttpSession session) {

        try {
            requireAdmin(session);

            if (id == null || id <= 0) {
                throw new IllegalArgumentException(
                        "实例ID不合法"
                );
            }

            LabSession labSession =
                    labSessionMapper.selectById(id);

            if (labSession == null) {
                throw new IllegalArgumentException(
                        "靶场实例记录不存在"
                );
            }

            if (!"DVWA".equals(labSession.getLabType())) {
                throw new IllegalArgumentException(
                        "当前实例类型不支持此操作"
                );
            }

            if (labSession.getTaskId() == null
                    || labSession.getHostPort() == null) {
                throw new IllegalArgumentException(
                        "实例记录缺少必要信息，无法停止"
                );
            }

            String expectedProjectName =
                    "lab_task_" + labSession.getTaskId();

            if (!expectedProjectName.equals(
                    labSession.getInstanceKey())) {
                throw new IllegalArgumentException(
                        "实例项目名称与任务记录不匹配"
                );
            }

            // 生成或更新当前实例的环境变量文件
            Path envFile = dockerComposeManager.prepareInstance(
                    expectedProjectName,
                    labSession.getHostPort()
            );

// 仅停止容器，保留容器、网络和数据卷
            dockerComposeManager.stop(
                    expectedProjectName,
                    envFile
            );

            // 更新实例状态
            labSession.setStatus("STOPPED");
            labSession.setStoppedAt(LocalDateTime.now());

            labSessionMapper.updateById(labSession);

            return ApiResponse.success(
                    "靶场实例已停止，数据库数据卷已保留",
                    null
            );

        } catch (IllegalArgumentException e) {
            return ApiResponse.error(e.getMessage());

        } catch (Exception e) {
            return ApiResponse.error(
                    "停止靶场实例失败：" + e.getMessage()
            );
        }
    }


    /**
     * 重新启动指定靶场实例。
     *
     * 复用原来的 Compose 项目、端口和数据库数据卷。
     */
    @PostMapping("/{id}/restart")
    public ApiResponse<Void> restart(
            @PathVariable Long id,
            HttpSession session) {

        LabSession labSession = null;

        try {
            requireAdmin(session);

            if (id == null || id <= 0) {
                throw new IllegalArgumentException("实例ID不合法");
            }

            labSession = labSessionMapper.selectById(id);

            if (labSession == null) {
                throw new IllegalArgumentException("靶场实例记录不存在");
            }

            if (!"DVWA".equals(labSession.getLabType())) {
                throw new IllegalArgumentException("当前实例类型不支持此操作");
            }

            if (labSession.getTaskId() == null
                    || labSession.getHostPort() == null) {
                throw new IllegalArgumentException("实例记录缺少必要信息");
            }

            String projectName = "lab_task_" + labSession.getTaskId();

            if (!projectName.equals(labSession.getInstanceKey())) {
                throw new IllegalArgumentException("实例项目名称与任务记录不匹配");
            }

            // 标记为启动中
            labSession.setStatus("STARTING");
            labSession.setLastError(null);
            labSessionMapper.updateById(labSession);

            // 使用原端口重新生成环境变量文件
            Path envFile = dockerComposeManager.prepareInstance(
                    projectName,
                    labSession.getHostPort()
            );

            // 启动原 Compose 项目，复用原数据卷
            dockerComposeManager.start(projectName, envFile);

            String webContainerId = dockerComposeManager.getContainerId(
                    projectName, envFile, "dvwa"
            );

            String dbContainerId = dockerComposeManager.getContainerId(
                    projectName, envFile, "db"
            );

            if (webContainerId == null || dbContainerId == null) {
                throw new IllegalStateException("未能获取完整的容器ID");
            }

            dockerComposeManager.waitForWebContainer(webContainerId);

            if (!dockerComposeManager.isContainerRunning(dbContainerId)) {
                throw new IllegalStateException("DVWA数据库容器未正常运行");
            }

            dockerComposeManager.waitForHttp(
                    "http://127.0.0.1:"
                            + labSession.getHostPort()
                            + "/setup.php"
            );

            // 更新容器信息和实例状态
            labSession.setWebContainerId(webContainerId);
            labSession.setDbContainerId(dbContainerId);
            labSession.setAccessUrl(
                    "http://localhost:" + labSession.getHostPort()
            );
            labSession.setStatus("RUNNING");
            labSession.setStartedAt(LocalDateTime.now());
            labSession.setStoppedAt(null);
            labSession.setLastError(null);

            labSessionMapper.updateById(labSession);

            return ApiResponse.success("靶场实例重新启动成功", null);

        } catch (IllegalArgumentException e) {
            return ApiResponse.error(e.getMessage());

        } catch (Exception e) {

            if (labSession != null) {
                labSession.setStatus("FAILED");
                labSession.setLastError(e.getMessage());
                labSessionMapper.updateById(labSession);
            }

            return ApiResponse.error(
                    "重新启动靶场实例失败：" + e.getMessage()
            );
        }
    }


    /**
     * 回收靶场实例的容器和网络。
     *
     * 保留数据库数据卷和实例记录。
     */
    @PostMapping("/{id}/reclaim")
    public ApiResponse<Void> reclaim(
            @PathVariable Long id,
            HttpSession session) {

        try {
            requireAdmin(session);

            if (id == null || id <= 0) {
                throw new IllegalArgumentException("实例ID不合法");
            }

            LabSession labSession =
                    labSessionMapper.selectById(id);

            if (labSession == null) {
                throw new IllegalArgumentException("靶场实例记录不存在");
            }

            if (!"DVWA".equals(labSession.getLabType())) {
                throw new IllegalArgumentException("当前实例类型不支持此操作");
            }

            if (labSession.getTaskId() == null
                    || labSession.getHostPort() == null) {
                throw new IllegalArgumentException("实例记录缺少必要信息");
            }

            String projectName =
                    "lab_task_" + labSession.getTaskId();

            if (!projectName.equals(labSession.getInstanceKey())) {
                throw new IllegalArgumentException(
                        "实例项目名称与任务记录不匹配"
                );
            }

            Path envFile = dockerComposeManager.prepareInstance(
                    projectName,
                    labSession.getHostPort()
            );

            // 删除容器和网络，但保留数据库数据卷
            dockerComposeManager.reclaim(
                    projectName,
                    envFile
            );

            labSession.setStatus("STOPPED");
            labSession.setStoppedAt(LocalDateTime.now());
            labSession.setWebContainerId(null);
            labSession.setDbContainerId(null);
            labSession.setLastError(null);

            labSessionMapper.updateById(labSession);

            return ApiResponse.success(
                    "实例容器和网络已回收，数据库数据卷已保留",
                    null
            );

        } catch (IllegalArgumentException e) {
            return ApiResponse.error(e.getMessage());

        } catch (Exception e) {
            return ApiResponse.error(
                    "回收靶场实例失败：" + e.getMessage()
            );
        }
    }





    /**
     * 管理员身份校验
     */
    private void requireAdmin(HttpSession session) {

        Object loginUser = session.getAttribute("loginUser");

        if (!(loginUser instanceof SysUser)) {
            throw new IllegalArgumentException(
                    "请先登录管理员账号"
            );
        }

        SysUser user = (SysUser) loginUser;

        if (!"ADMIN".equals(user.getRole())) {
            throw new IllegalArgumentException(
                    "只有管理员可以管理靶场实例"
            );
        }

        if (user.getId() == null) {
            throw new IllegalArgumentException(
                    "管理员信息无效，请重新登录"
            );
        }
    }
}
