// File: src/main/java/com/hmdp/config/security/jwt/JwtUtil.java
package com.hmdp.config.security.jwt;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;

public class JwtUtil {
    private static SecretKey key(String secret) {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public static String genAccess(String secret, long ttlMinutes, String userId, Map<String,Object> claims) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(userId)
                .setIssuedAt(new Date(now))
                .setExpiration(new Date(now + ttlMinutes * 60_000))
                .signWith(key(secret), SignatureAlgorithm.HS256)
                .compact();
    }

    public static String genRefresh(String secret, long ttlDays, String userId) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .setSubject(userId)
                .setIssuedAt(new Date(now))
                .setExpiration(new Date(now + ttlDays * 24 * 60 * 60_000))
                .signWith(key(secret), SignatureAlgorithm.HS256)
                .compact();
    }

    public static Jws<Claims> parse(String secret, String token) {
        return Jwts.parserBuilder().setSigningKey(key(secret)).build().parseClaimsJws(token);
    }
}
