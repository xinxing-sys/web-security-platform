package com.websecurity.platform.service;

import com.websecurity.platform.dto.LoginRequest;
import com.websecurity.platform.dto.RegisterRequest;
import com.websecurity.platform.entity.SysUser;

public interface UserService {

    SysUser register(RegisterRequest request);

    SysUser login(LoginRequest request);

    SysUser findByUsername(String username);
}