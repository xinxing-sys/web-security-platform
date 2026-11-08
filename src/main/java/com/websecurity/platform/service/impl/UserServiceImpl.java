
package com.websecurity.platform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.websecurity.platform.dto.LoginRequest;
import com.websecurity.platform.dto.RegisterRequest;
import com.websecurity.platform.dto.UserQueryRequest;
import com.websecurity.platform.entity.SysUser;
import com.websecurity.platform.mapper.SysUserMapper;
import com.websecurity.platform.service.UserService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserServiceImpl implements UserService {

    private final SysUserMapper userMapper;

    private final BCryptPasswordEncoder passwordEncoder =
            new BCryptPasswordEncoder();

    public UserServiceImpl(SysUserMapper userMapper) {
        this.userMapper = userMapper;
    }

    /**
     * 用户注册
     */
    @Override
    @Transactional
    public SysUser register(RegisterRequest request) {

        SysUser existingUser = findByUsername(request.getUsername());

        if (existingUser != null) {
            throw new IllegalArgumentException("用户名已经存在");
        }

        SysUser user = new SysUser();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setNickname(request.getNickname());
        user.setEmail(request.getEmail());
        user.setRole("STUDENT");
        user.setStatus(1);

        userMapper.insert(user);

        // 不向调用方返回密码摘要
        user.setPassword(null);

        return user;
    }

    /**
     * 用户登录
     */
    @Override
    public SysUser login(LoginRequest request) {

        SysUser user = findByUsername(request.getUsername());

        if (user == null) {
            throw new IllegalArgumentException("用户名或密码错误");
        }

        if (!passwordEncoder.matches(
                request.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("用户名或密码错误");
        }

        if (user.getStatus() == null || user.getStatus() != 1) {
            throw new IllegalArgumentException("该账号已被禁用");
        }

        user.setPassword(null);
        return user;
    }

    /**
     * 根据用户名查询用户
     */
    @Override
    public SysUser findByUsername(String username) {

        return userMapper.selectOne(
                new LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getUsername, username)
                        .last("LIMIT 1")
        );
    }

    /**
     * 管理员分页查询用户
     */
    @Override
    public IPage<SysUser> queryUsers(UserQueryRequest request) {

        if (request == null) {
            throw new IllegalArgumentException("查询参数不能为空");
        }

        // 获取分页参数，防止非法页码和每页数量
        int pageNum = request.getPageNum() == null
                ? 1 : request.getPageNum();

        int pageSize = request.getPageSize() == null
                ? 10 : request.getPageSize();

        if (pageNum < 1) {
            throw new IllegalArgumentException("页码必须大于等于1");
        }

        if (pageSize < 1 || pageSize > 100) {
            throw new IllegalArgumentException(
                    "每页数量必须在1到100之间");
        }

        // 创建分页对象
        Page<SysUser> page = new Page<>(pageNum, pageSize);

        // 创建查询条件
        LambdaQueryWrapper<SysUser> wrapper =
                new LambdaQueryWrapper<>();

        // 用户名模糊搜索
        if (request.getUsername() != null
                && !request.getUsername().trim().isEmpty()) {
            wrapper.like(
                    SysUser::getUsername,
                    request.getUsername().trim()
            );
        }

        // 昵称模糊搜索
        if (request.getNickname() != null
                && !request.getNickname().trim().isEmpty()) {
            wrapper.like(
                    SysUser::getNickname,
                    request.getNickname().trim()
            );
        }

        // 角色筛选
        if (request.getRole() != null
                && !request.getRole().trim().isEmpty()) {

            String role = request.getRole().trim().toUpperCase();

            if (!"STUDENT".equals(role)
                    && !"ADMIN".equals(role)) {
                throw new IllegalArgumentException(
                        "用户角色只能是STUDENT或ADMIN");
            }

            wrapper.eq(SysUser::getRole, role);
        }

        // 账号状态筛选
        if (request.getStatus() != null) {

            if (request.getStatus() != 0
                    && request.getStatus() != 1) {
                throw new IllegalArgumentException(
                        "用户状态只能为0或1");
            }

            wrapper.eq(
                    SysUser::getStatus,
                    request.getStatus()
            );
        }

        // 按用户ID倒序排列，最新注册的用户优先显示
        wrapper.orderByDesc(SysUser::getId);

        // 执行数据库分页查询
        IPage<SysUser> result =
                userMapper.selectPage(page, wrapper);

        // 清除所有用户的密码字段，避免密码摘要泄露
        result.getRecords().forEach(user ->
                user.setPassword(null)
        );

        return result;
    }


    /**
     * 根据用户ID查询用户详情
     */
    @Override
    public SysUser findById(Long id) {

        if (id == null || id <= 0) {
            throw new IllegalArgumentException("用户ID不合法");
        }

        SysUser user = userMapper.selectById(id);

        if (user == null) {
            throw new IllegalArgumentException("用户不存在");
        }

        // 不返回密码摘要
        user.setPassword(null);

        return user;
    }



    /**
     * 管理员修改用户启用/禁用状态
     */
    @Override
    @Transactional
    public void updateUserStatus(
            Long targetUserId,
            Integer status,
            Long operatorUserId) {

        if (targetUserId == null || targetUserId <= 0) {
            throw new IllegalArgumentException("用户ID不合法");
        }

        if (operatorUserId == null) {
            throw new IllegalArgumentException("当前操作用户不存在");
        }

        if (status == null || (status != 0 && status != 1)) {
            throw new IllegalArgumentException("用户状态只能为0或1");
        }

        // 禁止管理员禁用自己的账号
        if (targetUserId.equals(operatorUserId) && status == 0) {
            throw new IllegalArgumentException("不能禁用自己的管理员账号");
        }

        // 查询被修改的用户
        SysUser user = userMapper.selectById(targetUserId);

        if (user == null) {
            throw new IllegalArgumentException("用户不存在");
        }

        // 更新用户状态
        user.setStatus(status);

        int rows = userMapper.updateById(user);

        if (rows != 1) {
            throw new IllegalArgumentException("用户状态修改失败");
        }
    }


    /**
     * 管理员修改用户角色
     */
    @Override
    @Transactional
    public void updateUserRole(
            Long targetUserId,
            String role,
            Long operatorUserId) {

        if (targetUserId == null || targetUserId <= 0) {
            throw new IllegalArgumentException("用户ID不合法");
        }

        if (operatorUserId == null) {
            throw new IllegalArgumentException("当前操作用户不存在");
        }

        if (role == null || role.trim().isEmpty()) {
            throw new IllegalArgumentException("用户角色不能为空");
        }

        role = role.trim().toUpperCase();

        if (!"STUDENT".equals(role) && !"ADMIN".equals(role)) {
            throw new IllegalArgumentException(
                    "用户角色只能是STUDENT或ADMIN"
            );
        }

        // 禁止管理员修改自己的角色
        if (targetUserId.equals(operatorUserId)) {
            throw new IllegalArgumentException("不能修改自己的管理员角色");
        }

        // 查询被修改的用户
        SysUser user = userMapper.selectById(targetUserId);

        if (user == null) {
            throw new IllegalArgumentException("用户不存在");
        }

        // 更新用户角色
        user.setRole(role);

        int rows = userMapper.updateById(user);

        if (rows != 1) {
            throw new IllegalArgumentException("用户角色修改失败");
        }
    }



}
