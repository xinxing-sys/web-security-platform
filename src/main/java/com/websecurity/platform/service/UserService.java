
package com.websecurity.platform.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.websecurity.platform.dto.RegisterRequest;
import com.websecurity.platform.dto.LoginRequest;
import com.websecurity.platform.dto.UserQueryRequest;
import com.websecurity.platform.entity.SysUser;

public interface UserService {

    /**
     * 用户注册
     */
    SysUser register(RegisterRequest request);

    /**
     * 用户登录
     */
    SysUser login(LoginRequest request);

    /**
     * 根据用户名查询用户
     */
    SysUser findByUsername(String username);

    /**
     * 管理员分页查询用户
     */
    IPage<SysUser> queryUsers(UserQueryRequest request);

    /**
     * 根据用户ID查询用户详情
     */
    SysUser findById(Long id);




    /**
     * 管理员修改用户启用/禁用状态
     *
     * @param targetUserId 被修改用户ID
     * @param status       新状态：1启用，0禁用
     * @param operatorUserId 当前操作管理员ID
     */
    void updateUserStatus(
            Long targetUserId,
            Integer status,
            Long operatorUserId
    );


    /**
     * 管理员修改用户角色
     *
     * @param targetUserId 被修改用户ID
     * @param role         新角色
     * @param operatorUserId 当前操作管理员ID
     */
    void updateUserRole(
            Long targetUserId,
            String role,
            Long operatorUserId
    );


}
