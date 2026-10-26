package ru.ulstu.computerstore.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Service
public class JwtTokenService {

    @Value("${jwt.secret}")
    private String secret;

    public String generateToken(String username, List<String> roles, Map<String, Object> publicClaims) {
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        return Jwts.builder()
                .subject(username)
                .claims(publicClaims)          
                .claim("roles", roles)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 3600_000))
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }


    public String generateToken(String username, List<String> roles) {
        return generateToken(username, roles, Map.of());
    }
}