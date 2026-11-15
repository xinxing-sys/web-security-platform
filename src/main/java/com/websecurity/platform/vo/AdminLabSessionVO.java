
package com.websecurity.platform.vo;

import com.websecurity.platform.entity.LabSession;

import java.time.LocalDateTime;

public class AdminLabSessionVO {

    private Long id;
    private Long userId;
    private Long taskId;
    private String labType;
    private String instanceKey;
    private Integer hostPort;
    private String accessUrl;
    private String status;
    private boolean webRunning;
    private boolean dbRunning;
    private LocalDateTime startedAt;
    private LocalDateTime createdAt;
    private String lastError;

    public static AdminLabSessionVO fromEntity(
            LabSession entity,
            boolean webRunning,
            boolean dbRunning) {

        AdminLabSessionVO vo = new AdminLabSessionVO();

        vo.setId(entity.getId());
        vo.setUserId(entity.getUserId());
        vo.setTaskId(entity.getTaskId());
        vo.setLabType(entity.getLabType());
        vo.setInstanceKey(entity.getInstanceKey());
        vo.setHostPort(entity.getHostPort());
        vo.setAccessUrl(entity.getAccessUrl());
        vo.setStatus(entity.getStatus());
        vo.setWebRunning(webRunning);
        vo.setDbRunning(dbRunning);
        vo.setStartedAt(entity.getStartedAt());
        vo.setCreatedAt(entity.getCreatedAt());
        vo.setLastError(entity.getLastError());

        return vo;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
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

    public String getInstanceKey() {
        return instanceKey;
    }

    public void setInstanceKey(String instanceKey) {
        this.instanceKey = instanceKey;
    }

    public Integer getHostPort() {
        return hostPort;
    }

    public void setHostPort(Integer hostPort) {
        this.hostPort = hostPort;
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

    public boolean isWebRunning() {
        return webRunning;
    }

    public void setWebRunning(boolean webRunning) {
        this.webRunning = webRunning;
    }

    public boolean isDbRunning() {
        return dbRunning;
    }

    public void setDbRunning(boolean dbRunning) {
        this.dbRunning = dbRunning;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getLastError() {
        return lastError;
    }

    public void setLastError(String lastError) {
        this.lastError = lastError;
    }
}
