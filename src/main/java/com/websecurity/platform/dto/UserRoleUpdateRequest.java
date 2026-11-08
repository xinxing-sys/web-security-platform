
package com.websecurity.platform.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * 管理员修改用户角色的请求参数
 */
public class UserRoleUpdateRequest {

    /**
     * 用户角色：STUDENT或ADMIN
     */
    @NotBlank(message = "用户角色不能为空")
    @Pattern(
            regexp = "STUDENT|ADMIN",
            message = "用户角色只能是STUDENT或ADMIN"
    )
    private String role;

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }
}
