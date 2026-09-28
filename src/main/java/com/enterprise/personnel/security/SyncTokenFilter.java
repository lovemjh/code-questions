package com.enterprise.personnel.security;

import com.enterprise.personnel.common.Result;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * 外部同步接口业务 Token 校验：仅拦截 /api/sync/**，与 JWT 登录体系完全隔离
 * （不读 Authorization 头，JWT 过滤器也不识别 X-Sync-Token）。
 * <p>
 * fail-closed：sync.api.token 未配置时接口整体禁用（403），
 * 避免"空 Token 匹配空请求头"的穿透；校验失败 401。
 * Token 比较使用常量时间比较，防时序侧信道。
 */
@Slf4j
@Component
public class SyncTokenFilter extends OncePerRequestFilter {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static final String SYNC_PATH = "/api/sync";

    private static final String TOKEN_HEADER = "X-Sync-Token";

    private final String configuredToken;

    public SyncTokenFilter(@Value("${sync.api.token:}") String configuredToken) {
        this.configuredToken = configuredToken == null ? "" : configuredToken.trim();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (!isSyncRequest(request)) {
            filterChain.doFilter(request, response);
            return;
        }
        if (!StringUtils.hasText(configuredToken)) {
            log.warn("同步接口未配置 Token，已禁用: {}", request.getRequestURI());
            writeJson(response, HttpStatus.FORBIDDEN.value(), "同步接口未配置 Token，已禁用");
            return;
        }
        String token = request.getHeader(TOKEN_HEADER);
        if (!constantTimeEquals(token, configuredToken)) {
            log.warn("同步接口 Token 校验失败: {} {}", request.getMethod(), request.getRequestURI());
            writeJson(response, HttpStatus.UNAUTHORIZED.value(), "同步Token无效");
            return;
        }
        filterChain.doFilter(request, response);
    }

    private boolean isSyncRequest(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return uri.equals(SYNC_PATH) || uri.startsWith(SYNC_PATH + "/");
    }

    /** 常量时间比较：MessageDigest.isEqual 内部按长度取齐逐字节比较，不随前缀匹配提前返回 */
    private boolean constantTimeEquals(String actual, String expected) {
        if (actual == null || actual.isEmpty()) {
            return false;
        }
        return MessageDigest.isEqual(
                actual.getBytes(StandardCharsets.UTF_8),
                expected.getBytes(StandardCharsets.UTF_8));
    }

    /** 输出与业务接口一致的 Result JSON，便于调用方统一解析 */
    private void writeJson(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(OBJECT_MAPPER.writeValueAsString(Result.error(status, message)));
    }
}
