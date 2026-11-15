
package com.websecurity.platform.controller;

import com.websecurity.platform.common.ApiResponse;

import com.websecurity.platform.common.ApiResponse;
import com.websecurity.platform.entity.LabSession;
import com.websecurity.platform.entity.SysUser;
import com.websecurity.platform.service.LabInstanceService;
import com.websecurity.platform.service.LabSessionService;
import com.websecurity.platform.vo.LabSessionVO;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.*;




@RestController
@RequestMapping("/api/labs")
public class LabInstanceController {

    private final LabInstanceService labInstanceService;

    private final LabSessionService labSessionService;

    public LabInstanceController(
            LabInstanceService labInstanceService,
            LabSessionService labSessionService) {

        this.labInstanceService = labInstanceService;
        this.labSessionService = labSessionService;
    }

    /**
     * 启动或复用当前学生实验任务对应的DVWA实例。
     *
     * POST /api/labs/tasks/{taskId}/start
     */
    @PostMapping("/tasks/{taskId}/start")
    public ApiResponse<LabSessionVO> start(
            @PathVariable Long taskId,
            HttpSession session) {

        try {

            SysUser user = getStudent(session);

            LabSession labSession =
                    labInstanceService.startOrReuse(
                            user.getId(),
                            taskId
                    );

            return ApiResponse.success(
                    "DVWA靶场启动成功",
                    LabSessionVO.fromEntity(labSession)
            );

        } catch (IllegalArgumentException
                 | IllegalStateException e) {

            return ApiResponse.error(e.getMessage());

        } catch (Exception e) {

            return ApiResponse.error(
                    "DVWA靶场启动失败，请检查Docker服务"
            );
        }
    }

    /**
     * 查询当前学生指定任务的靶场实例。
     *
     * GET /api/labs/tasks/{taskId}
     */
    @GetMapping("/tasks/{taskId}")
    public ApiResponse<LabSessionVO> getMySession(
            @PathVariable Long taskId,
            HttpSession session) {

        try {

            SysUser user = getStudent(session);

            // 校验任务归属和实验类型
            labSessionService.validateTaskForLab(
                    user.getId(),
                    taskId
            );

            LabSession labSession =
                    labSessionService.getMySession(
                            user.getId(),
                            taskId
                    );

            if (labSession == null) {
                return ApiResponse.error(
                        "当前实验任务尚未创建靶场实例"
                );
            }

            return ApiResponse.success(
                    "查询成功",
                    LabSessionVO.fromEntity(labSession)
            );

        } catch (IllegalArgumentException
                 | IllegalStateException e) {

            return ApiResponse.error(e.getMessage());

        } catch (Exception e) {

            return ApiResponse.error(
                    "查询靶场实例失败"
            );
        }
    }

    /**
     * 获取当前登录学生。
     */
    private SysUser getStudent(HttpSession session) {

        Object loginUser = session.getAttribute("loginUser");

        if (!(loginUser instanceof SysUser)) {
            throw new IllegalArgumentException(
                    "请先登录"
            );
        }

        SysUser user = (SysUser) loginUser;

        if (!"STUDENT".equals(user.getRole())) {
            throw new IllegalArgumentException(
                    "只有学生用户可以操作实验靶场"
            );
        }

        if (user.getId() == null) {
            throw new IllegalArgumentException(
                    "用户信息无效，请重新登录"
            );
        }

        return user;
    }
}
