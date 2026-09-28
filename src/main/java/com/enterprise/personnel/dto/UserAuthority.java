package com.enterprise.personnel.dto;

import java.util.Collections;
import java.util.List;

/**
 * 用户的有效权限：账号是否仍存在 + 最新角色编码。
 * <p>
 * 角色不再只信 JWT 里的声明，而是每次请求从本地缓存（改角色时同步失效）读取，
 * 这样管理员调整角色后无需等 Token 过期即可生效，同时常规请求不会查库。
 */
public class UserAuthority {

    private final boolean exists;
    private final List<String> roles;

    private UserAuthority(boolean exists, List<String> roles) {
        this.exists = exists;
        this.roles = roles;
    }

    public static UserAuthority of(List<String> roles) {
        return new UserAuthority(true, roles == null ? Collections.emptyList() : roles);
    }

    /** 账号已不存在（被删除），旧 Token 立即失效 */
    public static UserAuthority missing() {
        return new UserAuthority(false, Collections.emptyList());
    }

    public boolean isExists() {
        return exists;
    }

    public List<String> getRoles() {
        return roles;
    }
}
