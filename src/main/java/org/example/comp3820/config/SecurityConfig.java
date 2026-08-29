package org.example.comp3820.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        // 在原型开发初期，先放行 API 端点以便于前端对接测试
        // 最终交付阶段需在此处接入 OAuth2 Resource Server 逻辑
        http.csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/support/**").permitAll()
                        .anyRequest().authenticated()
                );
        return http.build();
    }
}