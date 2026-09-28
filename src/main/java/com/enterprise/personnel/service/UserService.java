package com.enterprise.personnel.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.enterprise.personnel.dto.UserAuthority;
import com.enterprise.personnel.dto.UserSaveRequest;
import com.enterprise.personnel.entity.SysUser;

import java.util.List;

/**
 * 用户管理服务
 */
public interface UserService {

    /** 分页查询用户 */
    Page<SysUser> pageQuery(long pageNum, long pageSize, String username);

    /** 用户详情（含角色） */
    SysUser getDetail(Long id);

    /** 新增用户 */
    SysUser create(UserSaveRequest request);

    /** 编辑用户 */
    SysUser update(Long id, UserSaveRequest request);

    /** 删除用户 */
    void delete(Long id);

    /** 根据用户名查询 */
    SysUser getByUsername(String username);

    /** 查询用户角色编码列表（直接查库） */
    List<String> getRoleCodesByUserId(Long userId);

    /**
     * 查询用户当前有效权限（带本地缓存，改角色/删用户时同步失效）
     *
     * @param userId 用户ID
     */
    UserAuthority getAuthority(Long userId);

    /** 使指定用户的权限缓存失效（角色或账号变更后调用） */
    void evictAuthority(Long userId);
}
