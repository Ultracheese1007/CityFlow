// File: src/main/java/com/hmdp/config/security/SecurityConfig.java
package com.hmdp.config.security;

import com.hmdp.config.security.jwt.JwtAuthenticationFilter;
import com.hmdp.config.security.jwt.RestAuthenticationEntryPoint;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtFilter;
    private final RestAuthenticationEntryPoint entryPoint;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf().disable()
                .cors(Customizer.withDefaults())
                .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .and()
                .exceptionHandling().authenticationEntryPoint(entryPoint)
                .and()
                .authorizeHttpRequests(auth -> auth
                        // —— 新增白名单 ——
                        .antMatchers(HttpMethod.POST, "/voucher/seckill").permitAll()
                        .antMatchers(HttpMethod.POST, "/user/login").permitAll()
                        .antMatchers(HttpMethod.POST, "/user/code").permitAll()
                        // 静态与健康检查
                        .antMatchers("/", "/index.html", "/**/*.js", "/**/*.css", "/**/*.png", "/**/*.jpg",
                                "/actuator/**", "/error").permitAll()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}
