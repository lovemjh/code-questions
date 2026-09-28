package com.enterprise.personnel.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.enterprise.personnel.common.BusinessException;
import com.enterprise.personnel.dto.UserAuthority;
import com.enterprise.personnel.dto.UserSaveRequest;
import com.enterprise.personnel.entity.Role;
import com.enterprise.personnel.entity.SysUser;
import com.enterprise.personnel.entity.UserRole;
import com.enterprise.personnel.mapper.RoleMapper;
import com.enterprise.personnel.mapper.UserMapper;
import com.enterprise.personnel.mapper.UserRoleMapper;
import com.enterprise.personnel.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 用户管理服务实现
 */
@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, SysUser> implements UserService {

    private final UserRoleMapper userRoleMapper;
    private final RoleMapper roleMapper;
    private final PasswordEncoder passwordEncoder;

    /** 权限本地缓存：userId -> 权限快照。改角色/删用户时主动失效，TTL 仅作兜底 */
    private final Map<Long, AuthorityEntry> authorityCache = new ConcurrentHashMap<>();

    /** 权限缓存兜底有效期（毫秒），防止绕过应用直接改库后长期不生效 */
    private static final long AUTHORITY_TTL_MS = 60_000L;

    /** 密码最小长度 */
    private static final int MIN_PASSWORD_LENGTH = 6;

    @Override
    public Page<SysUser> pageQuery(long pageNum, long pageSize, String username) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<SysUser>()
                .like(StringUtils.hasText(username), SysUser::getUsername, username)
                .orderByAsc(SysUser::getId);
        Page<SysUser> page = page(new Page<>(pageNum, pageSize), wrapper);
        // 清除密码哈希，避免泄露
        page.getRecords().forEach(u -> u.setPasswordHash(null));
        // 补齐角色信息（否则列表“角色”列恒为空）
        fillRoles(page.getRecords());
        return page;
    }

    @Override
    public SysUser getDetail(Long id) {
        SysUser user = getById(id);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        user.setPasswordHash(null);
        fillRoles(List.of(user));
        return user;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SysUser create(UserSaveRequest request) {
        // 用户名唯一性校验
        if (lambdaQuery().eq(SysUser::getUsername, request.getUsername()).count() > 0) {
            throw new BusinessException("用户名已存在：" + request.getUsername());
        }
        if (!StringUtils.hasText(request.getPassword())) {
            throw new BusinessException("密码不能为空");
        }
        checkPasswordStrength(request.getPassword());
        SysUser user = new SysUser();
        user.setUsername(request.getUsername());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setDisplayName(request.getDisplayName());
        save(user);
        // 分配角色
        assignRoles(user.getId(), request.getRoleIds());
        evictAuthority(user.getId());
        user.setPasswordHash(null);
        fillRoles(List.of(user));
        return user;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SysUser update(Long id, UserSaveRequest request) {
        SysUser user = getById(id);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        // 防止管理员把自己降级：修改自己的角色时必须保留管理员角色
        if (id.equals(currentUserId()) && request.getRoleIds() != null) {
            Long adminRoleId = adminRoleId();
            if (adminRoleId != null && !request.getRoleIds().contains(adminRoleId)) {
                throw new BusinessException("不能移除自己的管理员角色");
            }
        }
        // 用户名唯一性校验（排除自己)
        if (!user.getUsername().equals(request.getUsername())) {
            if (lambdaQuery().eq(SysUser::getUsername, request.getUsername()).count() > 0) {
                throw new BusinessException("用户名已存在：" + request.getUsername());
            }
        }
        user.setUsername(request.getUsername());
        if (StringUtils.hasText(request.getPassword())) {
            checkPasswordStrength(request.getPassword());
            user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        }
        user.setDisplayName(request.getDisplayName());
        user.setUpdatedAt(LocalDateTime.now());
        updateById(user);
        // 重新分配角色：roleIds 为 null 表示不修改角色，避免误清空
        if (request.getRoleIds() != null) {
            userRoleMapper.delete(new LambdaQueryWrapper<UserRole>().eq(UserRole::getUserId, id));
            assignRoles(id, request.getRoleIds());
            evictAuthority(id);
        }
        user.setPasswordHash(null);
        fillRoles(List.of(user));
        return user;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        if (getById(id) == null) {
            throw new BusinessException("用户不存在");
        }
        // 防止管理员把自己删掉，导致系统再无管理员
        if (id.equals(currentUserId())) {
            throw new BusinessException("不能删除当前登录账号");
        }
        removeById(id);
        userRoleMapper.delete(new LambdaQueryWrapper<UserRole>().eq(UserRole::getUserId, id));
        evictAuthority(id);
    }

    @Override
    public SysUser getByUsername(String username) {
        return getOne(new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, username));
    }

    @Override
    public List<String> getRoleCodesByUserId(Long userId) {
        List<UserRole> userRoles = userRoleMapper.selectList(
                new LambdaQueryWrapper<UserRole>().eq(UserRole::getUserId, userId));
        if (CollectionUtils.isEmpty(userRoles)) {
            return Collections.emptyList();
        }
        Set<Long> roleIds = userRoles.stream().map(UserRole::getRoleId).collect(Collectors.toSet());
        return roleMapper.selectBatchIds(roleIds).stream()
                .map(Role::getCode)
                .collect(Collectors.toList());
    }

    @Override
    public UserAuthority getAuthority(Long userId) {
        if (userId == null) {
            return UserAuthority.missing();
        }
        long now = System.currentTimeMillis();
        AuthorityEntry cached = authorityCache.get(userId);
        if (cached != null && now - cached.loadedAt < AUTHORITY_TTL_MS) {
            return cached.authority;
        }
        SysUser user = getById(userId);
        UserAuthority authority = user == null
                ? UserAuthority.missing()
                : UserAuthority.of(getRoleCodesByUserId(userId));
        authorityCache.put(userId, new AuthorityEntry(authority, now));
        return authority;
    }

    @Override
    public void evictAuthority(Long userId) {
        if (userId != null) {
            authorityCache.remove(userId);
        }
    }

    /** 当前登录用户ID（由 JWT 过滤器写入安全上下文），未登录返回 null */
    private Long currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Object principal = authentication == null ? null : authentication.getPrincipal();
        return principal instanceof Long ? (Long) principal : null;
    }

    /** ADMIN 角色ID，角色表中不存在则返回 null */
    private Long adminRoleId() {
        Role admin = roleMapper.selectOne(new LambdaQueryWrapper<Role>().eq(Role::getCode, "ADMIN"));
        return admin == null ? null : admin.getId();
    }

    /** 密码强度校验：当前要求最小长度 */
    private void checkPasswordStrength(String password) {
        if (password.length() < MIN_PASSWORD_LENGTH) {
            throw new BusinessException("密码长度不能少于 " + MIN_PASSWORD_LENGTH + " 位");
        }
    }

    /** 权限缓存条目：权限快照 + 加载时间 */
    private static class AuthorityEntry {
        private final UserAuthority authority;
        private final long loadedAt;

        private AuthorityEntry(UserAuthority authority, long loadedAt) {
            this.authority = authority;
            this.loadedAt = loadedAt;
        }
    }

    /** 批量补齐用户的角色 ID 与角色名称 */
    private void fillRoles(List<SysUser> users) {
        if (CollectionUtils.isEmpty(users)) {
            return;
        }
        List<Long> userIds = users.stream().map(SysUser::getId).collect(Collectors.toList());
        List<UserRole> userRoles = userRoleMapper.selectList(
                new LambdaQueryWrapper<UserRole>().in(UserRole::getUserId, userIds));
        if (CollectionUtils.isEmpty(userRoles)) {
            users.forEach(u -> {
                u.setRoleIds(Collections.emptyList());
                u.setRoleNames(Collections.emptyList());
            });
            return;
        }
        Set<Long> roleIdSet = userRoles.stream().map(UserRole::getRoleId).collect(Collectors.toSet());
        Map<Long, String> roleNameById = roleMapper.selectBatchIds(roleIdSet).stream()
                .collect(Collectors.toMap(Role::getId, Role::getName, (a, b) -> a));
        Map<Long, List<Long>> roleIdsByUser = userRoles.stream()
                .collect(Collectors.groupingBy(UserRole::getUserId,
                        Collectors.mapping(UserRole::getRoleId, Collectors.toList())));
        for (SysUser user : users) {
            List<Long> ids = roleIdsByUser.getOrDefault(user.getId(), Collections.emptyList());
            user.setRoleIds(ids);
            user.setRoleNames(ids.stream().map(roleNameById::get)
                    .filter(Objects::nonNull).collect(Collectors.toList()));
        }
    }

    /** 分配角色 */
    private void assignRoles(Long userId, List<Long> roleIds) {
        if (CollectionUtils.isEmpty(roleIds)) {
            return;
        }
        for (Long roleId : roleIds) {
            if (roleMapper.selectById(roleId) == null) {
                throw new BusinessException("角色不存在：" + roleId);
            }
            UserRole userRole = new UserRole();
            userRole.setUserId(userId);
            userRole.setRoleId(roleId);
            userRoleMapper.insert(userRole);
        }
    }
}
