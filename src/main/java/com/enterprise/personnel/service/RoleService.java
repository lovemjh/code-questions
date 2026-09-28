package com.enterprise.personnel.service;

import com.enterprise.personnel.entity.Role;

import java.util.List;

/**
 * 角色管理服务
 */
public interface RoleService {

    /** 全量角色列表 */
    List<Role> listAll();
}
