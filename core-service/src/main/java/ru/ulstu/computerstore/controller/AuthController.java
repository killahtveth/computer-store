package ru.ulstu.computerstore.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import ru.ulstu.computerstore.security.JwtTokenService;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final JwtTokenService tokenService;

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String password = body.get("password");

        if ("admin".equals(username) && "admin".equals(password)) {
            Map<String, Object> claims = new LinkedHashMap<>();
            claims.put("userId", 1);
            claims.put("email", "admin@computerstore.local");
            claims.put("fullName", "Администратор Системы");
            claims.put("department", "IT");

            String token = tokenService.generateToken(
                    username, List.of("ADMIN", "USER"), claims);
            return ResponseEntity.ok(Map.of("token", token));
        }
        if ("user".equals(username) && "user".equals(password)) {
            Map<String, Object> claims = new LinkedHashMap<>();
            claims.put("userId", 2);
            claims.put("email", "user@computerstore.local");
            claims.put("fullName", "Иван Иванов");
            claims.put("department", "Sales");

            String token = tokenService.generateToken(
                    username, List.of("USER"), claims);
            return ResponseEntity.ok(Map.of("token", token));
        }
        return ResponseEntity.status(401).build();
    }

    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> me(@AuthenticationPrincipal Jwt jwt) {
        Map<String, Object> profile = new LinkedHashMap<>();
        profile.put("sub", jwt.getSubject());
        profile.put("userId", jwt.getClaim("userId"));
        profile.put("email", jwt.getClaim("email"));
        profile.put("fullName", jwt.getClaim("fullName"));
        profile.put("department", jwt.getClaim("department"));
        profile.put("roles", jwt.getClaimAsStringList("roles"));
        profile.put("issuedAt", jwt.getIssuedAt());
        profile.put("expiresAt", jwt.getExpiresAt());
        return ResponseEntity.ok(profile);
    }
}