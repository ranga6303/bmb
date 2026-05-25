package com.example.demo.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import javax.crypto.SecretKey;

@Component
public class JwtUtil {
    private final SecretKey signingKey;
    private final long expirationMs;

    public JwtUtil(
        @Value("${app.jwt.secret}") String base64Secret,
        @Value("${app.jwt.expiration-ms}") long expirationMs
    ) {
        this.signingKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(base64Secret));
        this.expirationMs = expirationMs;
    }

    public String generateToken(UserDetails userDetails, Long userId, String role, Long sessionId, String deviceId) {
        Instant now = Instant.now();
        Instant exp = now.plusMillis(expirationMs);

        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("role", role);
        claims.put("sessionId", sessionId);
        claims.put("deviceId", deviceId);

        return Jwts.builder()
            .claims(claims)
            .subject(userDetails.getUsername())
            .issuedAt(Date.from(now))
            .expiration(Date.from(exp))
            .signWith(signingKey, Jwts.SIG.HS256)
            .compact();
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public Long extractUserId(String token) {
        return extractClaim(token, claims -> claims.get("userId", Long.class));
    }

    public String extractRole(String token) {
        return extractClaim(token, claims -> claims.get("role", String.class));
    }

    public Long extractSessionId(String token) {
        return extractClaim(token, claims -> claims.get("sessionId", Long.class));
    }

    public String extractDeviceId(String token) {
        return extractClaim(token, claims -> claims.get("deviceId", String.class));
    }

    public Date extractIssuedAt(String token) {
        return extractClaim(token, Claims::getIssuedAt);
    }

    public boolean validateToken(String token, UserDetails userDetails) {
        String username = extractUsername(token);
        if (!username.equals(userDetails.getUsername())) {
            return false;
        }

        Date expiration = extractClaim(token, Claims::getExpiration);
        if (expiration.before(new Date())) {
            return false;
        }

        if (userDetails instanceof AppUserDetails appUserDetails) {
            LocalDateTime lastPasswordChange = appUserDetails.getLastPasswordChange();
            if (lastPasswordChange != null) {
                LocalDateTime issuedAt = extractIssuedAt(token).toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
                if (!issuedAt.isAfter(lastPasswordChange)) {
                    return false;
                }
            }
        }

        return true;
    }

    private <T> T extractClaim(String token, Function<Claims, T> extractor) {
        Claims claims = Jwts.parser().verifyWith(signingKey).build().parseSignedClaims(token).getPayload();
        return extractor.apply(claims);
    }
}
