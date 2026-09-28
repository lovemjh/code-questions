package com.enterprise.personnel.service;

import com.enterprise.personnel.dto.LoginRequest;
import com.enterprise.personnel.dto.LoginResponse;

/**
 * 认证服务
 */
public interface AuthService {

    /**
     * 用户登录，成功返回 JWT Token
     */
    LoginResponse login(LoginRequest request);
}
