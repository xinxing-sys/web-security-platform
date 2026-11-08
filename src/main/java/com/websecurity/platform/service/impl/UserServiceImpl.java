package com.websecurity.platform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.websecurity.platform.dto.LoginRequest;
import com.websecurity.platform.dto.RegisterRequest;
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

    @Override
    public SysUser findByUsername(String username) {

        return userMapper.selectOne(
                new LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getUsername, username)
                        .last("LIMIT 1")
        );
    }
}