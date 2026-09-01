package com.dotlabs.moneytransfer.security;

import com.dotlabs.moneytransfer.entity.User;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class JwtTokenProvider {

    @Value("${app.jwt.secret:404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970}")
    private String jwtSecret;

    @Value("${app.jwt.expiration-ms:86400000}")
    private long jwtExpirationMs;

    public String generateToken(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        return generateTokenForUser(user);
    }

    public String generateTokenForUser(User user) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpirationMs);

        Map<String, Object> claims = new HashMap<>();
        claims.put("role", user.getRole().name());
        claims.put("accountNumber", user.getAccountNumber());
        claims.put("email", user.getEmail());

        return Jwts.builder()
                .subject(user.getUsername())
                .claims(claims)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSigningKey(), Jwts.SIG.HS256)
                .compact();
    }

    public String getUsernameFromToken(String token) {
        return getClaims(token).getSubject();
    }

    public String getAccountNumberFromToken(String token) {
        return (String) getClaims(token).get("accountNumber");
    }

    public boolean validateToken(String token) {
        try {
            getClaims(token);
            return true;
        } catch (MalformedJwtException ex) {
            log.error("Invalid JWT token: {}", ex.getMessage());
        } catch (ExpiredJwtException ex) {
            log.error("Expired JWT token: {}", ex.getMessage());
        } catch (UnsupportedJwtException ex) {
            log.error("Unsupported JWT token: {}", ex.getMessage());
        } catch (IllegalArgumentException ex) {
            log.error("JWT claims string is empty: {}", ex.getMessage());
        } catch (JwtException ex) {
            log.error("JWT validation error: {}", ex.getMessage());
        }
        return false;
    }

    public long getExpirationMs() {
        return jwtExpirationMs;
    }

    private Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(jwtSecret);
        if (keyBytes.length < 32) {
            // Fallback for raw string secrets
            return Keys.hmacShaKeyFor(jwtSecret.getBytes());
        }
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
