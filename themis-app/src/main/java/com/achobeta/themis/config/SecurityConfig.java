package com.achobeta.themis.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * 安全配置类
 * @author AckenieoT
 * @date 2025/10/30 上午10:20
 */
@Configuration
public class SecurityConfig {
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // 禁用HTTP Basic认证
                .httpBasic(httpBasic -> httpBasic.disable())
                // 关闭CSRF保护
                .csrf(csrf -> csrf.disable())
                // 配置接口访问权限
                .authorizeHttpRequests(auth -> auth
                        // 允许登录接口匿名访问（无需认证即可调用）
                        .requestMatchers("/api/user/login").permitAll()
                        .anyRequest().permitAll()  // 临时全放开
                );

        return http.build();
    }
}
