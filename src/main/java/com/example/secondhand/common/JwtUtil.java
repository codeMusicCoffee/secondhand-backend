/*
 * @Author: 'txy' '841067099@qq.com'
 * @Date: 2025-12-10 14:14:13
 * @LastEditors: 'txy' '841067099@qq.com'
 * @LastEditTime: 2025-12-18 09:53:49
 * @FilePath: \secondhand-try\backend\secondhand\src\main\java\com\example\secondhand\common\JwtUtil.java
 * @Description: 这是默认设置,请设置`customMade`, 打开koroFileHeader查看配置 进行设置: https://github.com/OBKoro1/koro1FileHeader/wiki/%E9%85%8D%E7%BD%AE
 */
package com.example.secondhand.common;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
public class JwtUtil {

    private static final String SECRET = "change_this_to_a_long_random_secret_at_least_32_chars!";
    private static final long EXPIRATION_MS = 24L * 60 * 60 * 1000; // 24小时

    private static SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
    }

    /** 生成 Token，带 userId + username */
    public static String generateToken(Long userId, String username) {
        Date now = new Date();

        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("username", username);

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(username)
                .setIssuedAt(now)
                .setExpiration(new Date(now.getTime() + EXPIRATION_MS))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    /** 从 token 获取 userId */
    public static Long getUserId(String token) {
        try {
            Claims claims = Jwts.parser()
                    .setSigningKey(getSigningKey())
                    .parseClaimsJws(token)
                    .getBody();

            Object uid = claims.get("userId");
            if (uid != null) {
                return Long.parseLong(uid.toString());
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    /** 从 token 获取 username */
    public static String getUsername(String token) {
        try {
            Claims claims = Jwts.parser()
                    .setSigningKey(getSigningKey())
                    .parseClaimsJws(token)
                    .getBody();
            return claims.get("username").toString();
        } catch (Exception e) {
            return null;
        }
    }

    /** 校验 token */
    public static boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .setSigningKey(getSigningKey())
                    .parseClaimsJws(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
