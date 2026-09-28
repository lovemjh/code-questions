package com.enterprise.personnel.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

/**
 * 登录响应
 */
@Data
@AllArgsConstructor
public class LoginResponse {

    /** JWT Token */
    private String token;

    /** 用户名 */
    private String username;

    /** 显示名称 */
    private String displayName;

    /** 角色编码列表 */
    private List<String> roles;
}
