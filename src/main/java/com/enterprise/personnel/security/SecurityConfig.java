package com.enterprise.personnel.security;

import com.enterprise.personnel.common.Result;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Spring Security 配置：无状态 JWT 认证 + 按角色（ADMIN / USER）授权
 *
 * Token 只用于确认身份；角色由 JwtAuthenticationFilter 从服务端权限缓存还原
 * （改角色立即生效、删账号后旧 Token 立即失效），与用户名无关：
 * 任何被授予 ADMIN 角色的用户都能访问管理员接口。
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf().disable()
            .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            .and()
            .authorizeRequests()
                // 登录接口与 H2 控制台无需认证
                .antMatchers("/api/auth/**", "/h2-console/**").permitAll()
                // 外部人员同步接口：由 SyncTokenFilter 校验独立业务 Token，不走 JWT
                .antMatchers("/api/sync/**").permitAll()
                // 用户管理、角色管理：仅管理员
                .antMatchers("/api/users/**", "/api/roles/**").hasAuthority("ADMIN")
                // 人员上级调整：仅管理员
                .antMatchers(HttpMethod.PUT, "/api/persons/**").hasAuthority("ADMIN")
                // 标签库维护：仅管理员
                .antMatchers(HttpMethod.POST, "/api/tags/**").hasAuthority("ADMIN")
                .antMatchers(HttpMethod.PUT, "/api/tags/**").hasAuthority("ADMIN")
                .antMatchers(HttpMethod.DELETE, "/api/tags/**").hasAuthority("ADMIN")
                // 标签导出：仅管理员（导入已由上面的 POST 规则覆盖）
                .antMatchers(HttpMethod.GET, "/api/tags/export").hasAuthority("ADMIN")
                // 其余接口：登录即可访问（普通用户可查询）
                .anyRequest().authenticated()
            .and()
            .headers().frameOptions().disable()
            .and()
            // 未登录 / Token 失效：401，返回统一 Result 结构
            .exceptionHandling()
                .authenticationEntryPoint((request, response, authException) ->
                        writeJson(response, HttpStatus.UNAUTHORIZED.value(), "未登录或登录已过期"))
                // 已登录但角色不足：403
                .accessDeniedHandler((request, response, accessDeniedException) ->
                        writeJson(response, HttpStatus.FORBIDDEN.value(), "权限不足，该操作需要管理员角色"))
            .and()
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    /** 输出与业务接口一致的 Result JSON，便于前端拦截器统一提示与跳转 */
    private void writeJson(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(OBJECT_MAPPER.writeValueAsString(Result.error(status, message)));
    }
}
