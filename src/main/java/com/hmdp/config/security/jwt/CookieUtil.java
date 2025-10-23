// File: src/main/java/com/hmdp/config/security/jwt/CookieUtil.java
package com.hmdp.config.security.jwt;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

public class CookieUtil {
    public static void addHttpOnlyCookie(HttpServletResponse resp, String name, String value,
                                         int maxAgeSeconds, String domain,
                                         boolean secure, String sameSite) {
        Cookie cookie = new Cookie(name, value);
        cookie.setHttpOnly(true);
        cookie.setMaxAge(maxAgeSeconds);
        cookie.setPath("/");
        if (domain != null && !domain.isEmpty()) cookie.setDomain(domain);
        cookie.setSecure(secure);
        resp.addCookie(cookie);
        // 同步补一个包含 SameSite 的 Set-Cookie（兼容性更好）
        StringBuilder sb = new StringBuilder();
        sb.append(name).append("=").append(value)
                .append("; Path=/")
                .append("; Max-Age=").append(maxAgeSeconds)
                .append("; HttpOnly");
        if (domain != null && !domain.isEmpty()) sb.append("; Domain=").append(domain);
        if (secure) sb.append("; Secure");
        if (sameSite != null && !sameSite.isEmpty()) sb.append("; SameSite=").append(sameSite);
        resp.addHeader("Set-Cookie", sb.toString());
    }
}