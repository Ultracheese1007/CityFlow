// File: src/main/java/com/hmdp/config/security/jwt/JwtProps.java
package com.hmdp.config.security.jwt;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtProps {
    // HS256 对称密钥（本地开发直接写，生产请走环境变量/密钥管理）
    private String secret = "CHANGE_ME_TO_A_LONG_RANDOM_SECRET_64+_CHARS";
    // Access 有效期（分钟）
    private long accessTtlMinutes = 30;
    // Refresh 有效期（天）
    private long refreshTtlDays = 14;

    // HttpOnly Cookie 配置
    private String refreshCookieName = "REFRESH_TOKEN";
    private String cookieDomain;      // 本地可留空
    private boolean cookieSecure = false; // 生产必须 true
    private String cookieSameSite = "Lax"; // 跨站需 None+Secure
}
