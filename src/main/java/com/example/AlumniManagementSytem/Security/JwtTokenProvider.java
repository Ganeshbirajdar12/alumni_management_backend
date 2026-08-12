package com.example.AlumniManagementSytem.Security;

import com.example.AlumniManagementSytem.enums.TokenType;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class JwtTokenProvider {

    private static final Logger log = LoggerFactory.getLogger(JwtTokenProvider.class);

    @Value("${jwt.secret}")
    private String jwtSecret;

    // Getters
    @Getter
    @Value("${jwt.access-token-expiration}")
    private long accessTokenExpiration;

    @Getter
    @Value("${jwt.refresh-token-expiration}")
    private long refreshTokenExpiration;

    private SecretKey key;

    @PostConstruct
    public void init() {
        log.info("Initializing JWT Provider...");
        try {
            byte[] keyBytes = Decoders.BASE64.decode(jwtSecret);
            log.info("Key bytes length: {}", keyBytes.length);
            this.key = Keys.hmacShaKeyFor(keyBytes);
            log.info("JWT Provider initialized successfully");
        } catch (Exception e) {
            log.error("Failed to initialize JWT Provider: {}", e.getMessage());
            throw new RuntimeException("JWT initialization failed", e);
        }
    }

    /**
     * Generate access token from Authentication object
     */
    public String generateAccessToken(Authentication authentication) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        return generateAccessToken(
                userDetails.getUsername(),
                userDetails.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .collect(Collectors.toSet())
        );
    }

    /**
     * Generate access token for email
     */
    public String generateAccessToken(String email) {
        return generateAccessToken(email, null);
    }

    /**
     * Generate access token with authorities
     */
    public String generateAccessToken(String email, Collection<String> authorities) {
        log.debug("Generating access token for: {}", email);

        Date now = new Date();
        Date expiry = new Date(now.getTime() + accessTokenExpiration);

        var builder = Jwts.builder()
                .subject(email)
                .id(UUID.randomUUID().toString())
                .issuedAt(now)
                .expiration(expiry)
                .claim("type", TokenType.ACCESS_TOKEN.name())
                .claim("email", email);

        if (authorities != null && !authorities.isEmpty()) {
            builder.claim("authorities", authorities);
        }

        return builder.signWith(key).compact();
    }

    /**
     * Generate refresh token
     */
    public String generateRefreshToken(String email) {
        log.debug("Generating refresh token for: {}", email);

        Date now = new Date();
        Date expiry = new Date(now.getTime() + refreshTokenExpiration);

        return Jwts.builder()
                .subject(email)
                .id(UUID.randomUUID().toString())
                .issuedAt(now)
                .expiration(expiry)
                .claim("type", TokenType.REFRESH_TOKEN.name())
                .claim("email", email)
                .signWith(key)
                .compact();
    }

    /**
     * Generate password reset token
     */
    public String generatePasswordResetToken(String email) {
        log.debug("Generating password reset token for: {}", email);

        Date now = new Date();
        Date expiry = new Date(now.getTime() + 3600000); // 1 hour

        return Jwts.builder()
                .subject(email)
                .id(UUID.randomUUID().toString())
                .issuedAt(now)
                .expiration(expiry)
                .claim("type", TokenType.PASSWORD_RESET_TOKEN.name())
                .claim("email", email)
                .claim("purpose", "PASSWORD_RESET")
                .signWith(key)
                .compact();
    }

    /**
     * Generate email verification token
     */
    public String generateEmailVerificationToken(String email) {
        log.debug("Generating email verification token for: {}", email);

        Date now = new Date();
        Date expiry = new Date(now.getTime() + 86400000); // 24 hours

        return Jwts.builder()
                .subject(email)
                .id(UUID.randomUUID().toString())
                .issuedAt(now)
                .expiration(expiry)
                .claim("type", TokenType.EMAIL_VERIFICATION_TOKEN.name())
                .claim("email", email)
                .claim("purpose", "EMAIL_VERIFICATION")
                .signWith(key)
                .compact();
    }

    /**
     * Extract email from token
     */
    public String getEmailFromToken(String token) {
        try {
            Claims claims = parseToken(token);
            return claims.getSubject();
        } catch (Exception e) {
            log.error("Error extracting email from token: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Extract token type from token
     */
    public String getTokenType(String token) {
        try {
            Claims claims = parseToken(token);
            return claims.get("type", String.class);
        } catch (Exception e) {
            log.error("Error extracting token type: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Extract token ID from token
     */
    public String getTokenId(String token) {
        try {
            Claims claims = parseToken(token);
            return claims.getId();
        } catch (Exception e) {
            log.error("Error extracting token ID: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Extract all claims from token
     */
    public Claims getAllClaims(String token) {
        return parseToken(token);
    }

    /**
     * Extract expiration date from token
     */
    public Date getExpirationDateFromToken(String token) {
        try {
            Claims claims = parseToken(token);
            return claims.getExpiration();
        } catch (Exception e) {
            log.error("Error extracting expiration date: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Check if token is expired
     */
    public boolean isTokenExpired(String token) {
        try {
            Date expiration = getExpirationDateFromToken(token);
            return expiration != null && expiration.before(new Date());
        } catch (Exception e) {
            return true;
        }
    }

    /**
     * Validate token
     */
    public boolean validateToken(String token) {
        if (token == null || token.isEmpty()) {
            log.warn("Token is null or empty");
            return false;
        }

        try {
            parseToken(token);
            return true;
        } catch (Exception e) {
            log.error("Token validation failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Get remaining validity in milliseconds
     */
    public long getTokenRemainingValidity(String token) {
        try {
            Date expiration = getExpirationDateFromToken(token);
            if (expiration != null) {
                return expiration.getTime() - System.currentTimeMillis();
            }
        } catch (Exception e) {
            log.error("Error getting token remaining validity: {}", e.getMessage());
        }
        return 0;
    }

    /**
     * Parse token - private helper method
     */
    private Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}