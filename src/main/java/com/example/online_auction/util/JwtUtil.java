// src/main/java/com/example/online_auction/util/JwtUtil.java
package com.example.online_auction.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
public class JwtUtil {
    private final SecretKey secretKey;

    @Value("${jwt.expiration}")
    private Long expiration;

    public JwtUtil(@Value("${jwt.secret}") String secret) {
        // Tạo SecretKey từ chuỗi secret (ít nhất 512 bits nếu đủ dài, hoặc dùng Keys)
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes()); // Tự động tạo khóa 512 bits nếu secret không đủ
    }

    public String generateToken(String username, String role) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", username);
        claims.put("role", role);

        return Jwts.builder()
            .setClaims(claims)
            .setIssuedAt(new Date())
            .setExpiration(new Date(System.currentTimeMillis() + expiration))
            .signWith(secretKey, SignatureAlgorithm.HS512) // Sử dụng SecretKey
            .compact();
    }

    public String getUsernameFromToken(String token) {
        Claims claims = Jwts.parser()
            .setSigningKey(secretKey) // Sử dụng SecretKey
            .parseClaimsJws(token)
            .getBody();
        return claims.get("sub", String.class);
    }

    public String getRoleFromToken(String token) {
        Claims claims = Jwts.parser()
            .setSigningKey(secretKey) // Sử dụng SecretKey
            .parseClaimsJws(token)
            .getBody();
        return claims.get("role", String.class);
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                .setSigningKey(secretKey) // Sử dụng SecretKey
                .parseClaimsJws(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean validateToken(String token, UserDetails userDetails) {
        final String username = getUsernameFromToken(token);
        return username.equals(userDetails.getUsername()) && validateToken(token);
    }
}