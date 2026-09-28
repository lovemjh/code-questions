package com.enterprise.personnel.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.enterprise.personnel.entity.Role;
import com.enterprise.personnel.mapper.RoleMapper;
import com.enterprise.personnel.service.RoleService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 角色管理服务实现
 */
@Service
public class RoleServiceImpl extends ServiceImpl<RoleMapper, Role> implements RoleService {

    @Override
    public List<Role> listAll() {
        return list(new LambdaQueryWrapper<Role>().orderByAsc(Role::getId));
    }
}
