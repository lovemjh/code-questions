package com.enterprise.personnel.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.enterprise.personnel.common.Result;
import com.enterprise.personnel.dto.UserSaveRequest;
import com.enterprise.personnel.entity.SysUser;
import com.enterprise.personnel.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户管理 API
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /** 分页查询用户 */
    @GetMapping
    public Result<Page<SysUser>> page(@RequestParam(defaultValue = "1") long pageNum,
                                      @RequestParam(defaultValue = "10") long pageSize,
                                      @RequestParam(required = false) String username) {
        return Result.ok(userService.pageQuery(pageNum, pageSize, username));
    }

    /** 用户详情 */
    @GetMapping("/{id}")
    public Result<SysUser> detail(@PathVariable Long id) {
        return Result.ok(userService.getDetail(id));
    }

    /** 新增用户 */
    @PostMapping
    public Result<SysUser> create(@Validated @RequestBody UserSaveRequest request) {
        return Result.ok(userService.create(request));
    }

    /** 编辑用户 */
    @PutMapping("/{id}")
    public Result<SysUser> update(@PathVariable Long id, @Validated @RequestBody UserSaveRequest request) {
        return Result.ok(userService.update(id, request));
    }

    /** 删除用户 */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        userService.delete(id);
        return Result.ok();
    }
}
