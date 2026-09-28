package com.enterprise.personnel.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 密码编码器配置。
 * <p>
 * 单独成类而不放在 {@link SecurityConfig} 里：SecurityConfig 依赖 JwtAuthenticationFilter，
 * 而业务服务依赖 PasswordEncoder，放一起会形成 循环依赖。
 */
@Configuration
public class PasswordEncoderConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
