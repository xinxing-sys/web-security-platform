
package com.websecurity.platform.vo;

import com.websecurity.platform.entity.LabSession;

import java.time.LocalDateTime;

public class LabSessionVO {

    private Long taskId;

    private String labType;

    private String accessUrl;

    private String status;

    private LocalDateTime startedAt;

    private LocalDateTime expiresAt;

    public static LabSessionVO fromEntity(LabSession session) {

        if (session == null) {
            return null;
        }

        LabSessionVO vo = new LabSessionVO();

        vo.setTaskId(session.getTaskId());
        vo.setLabType(session.getLabType());
        vo.setAccessUrl(session.getAccessUrl());
        vo.setStatus(session.getStatus());
        vo.setStartedAt(session.getStartedAt());
        vo.setExpiresAt(session.getExpiresAt());

        return vo;
    }

    public Long getTaskId() {
        return taskId;
    }

    public void setTaskId(Long taskId) {
        this.taskId = taskId;
    }

    public String getLabType() {
        return labType;
    }

    public void setLabType(String labType) {
        this.labType = labType;
    }

    public String getAccessUrl() {
        return accessUrl;
    }

    public void setAccessUrl(String accessUrl) {
        this.accessUrl = accessUrl;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }
}
