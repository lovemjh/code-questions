package com.enterprise.personnel.controller;

import com.enterprise.personnel.common.Result;
import com.enterprise.personnel.entity.Role;
import com.enterprise.personnel.service.RoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 角色管理 API
 */
@RestController
@RequestMapping("/api/roles")
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;

    /** 全量角色列表 */
    @GetMapping
    public Result<List<Role>> list() {
        return Result.ok(roleService.listAll());
    }
}
