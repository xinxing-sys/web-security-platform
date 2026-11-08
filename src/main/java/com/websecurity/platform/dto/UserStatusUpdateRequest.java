
package com.websecurity.platform.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * 管理员修改用户状态的请求参数
 */
public class UserStatusUpdateRequest {

    /**
     * 用户状态：1启用，0禁用
     */
    @NotNull(message = "用户状态不能为空")
    @Min(value = 0, message = "用户状态只能为0或1")
    @Max(value = 1, message = "用户状态只能为0或1")
    private Integer status;

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }
}
