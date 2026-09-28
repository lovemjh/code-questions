package com.enterprise.personnel.service.impl;

import com.enterprise.personnel.common.BusinessException;
import com.enterprise.personnel.dto.LoginRequest;
import com.enterprise.personnel.dto.LoginResponse;
import com.enterprise.personnel.entity.SysUser;
import com.enterprise.personnel.security.JwtUtils;
import com.enterprise.personnel.service.AuthService;
import com.enterprise.personnel.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 认证服务实现
 */
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;

    @Override
    public LoginResponse login(LoginRequest request) {
        SysUser user = userService.getByUsername(request.getUsername());
        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BusinessException("用户名或密码错误");
        }
        List<String> roles = userService.getRoleCodesByUserId(user.getId());
        String token = jwtUtils.generateToken(user.getId(), user.getUsername(), roles);
        return new LoginResponse(token, user.getUsername(), user.getDisplayName(), roles);
    }
}
