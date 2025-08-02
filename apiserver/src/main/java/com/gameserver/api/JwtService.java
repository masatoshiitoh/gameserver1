package com.gameserver.api;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.User;
import io.vertx.ext.auth.AuthProvider;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.auth.jwt.JWTAuthOptions;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

public class JwtService {
    
    private final JWTAuth jwtAuth;
    private static final String SECRET_KEY = "gameserver-secret-key-2024";
    private static final int TOKEN_EXPIRY_HOURS = 24;
    
    public JwtService(Vertx vertx) {
        JWTAuthOptions config = new JWTAuthOptions()
            .addJwk(new JsonObject()
                .put("kty", "oct")
                .put("k", Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(SECRET_KEY.getBytes(StandardCharsets.UTF_8)))
                .put("alg", "HS256"));
        
        this.jwtAuth = JWTAuth.create(vertx, config);
    }
    
    public String generateToken(int userId, String username) {
        long now = Instant.now().getEpochSecond();
        long exp = Instant.now().plus(TOKEN_EXPIRY_HOURS, ChronoUnit.HOURS).getEpochSecond();
        
        JsonObject payload = new JsonObject()
            .put("userId", userId)
            .put("username", username)
            .put("iat", now)
            .put("exp", exp);
        
        try {
            return jwtAuth.generateToken(payload);
        } catch (Exception e) {
            // Fallback to manual JWT generation if Vert.x JWT fails
            return generateTokenManually(payload);
        }
    }
    
    private String generateTokenManually(JsonObject payload) {
        try {
            JsonObject header = new JsonObject()
                .put("alg", "HS256")
                .put("typ", "JWT");
            
            String encodedHeader = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(header.encode().getBytes(StandardCharsets.UTF_8));
            String encodedPayload = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(payload.encode().getBytes(StandardCharsets.UTF_8));
            
            String data = encodedHeader + "." + encodedPayload;
            
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(SECRET_KEY.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKeySpec);
            byte[] signature = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            String encodedSignature = Base64.getUrlEncoder().withoutPadding().encodeToString(signature);
            
            return data + "." + encodedSignature;
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate JWT token", e);
        }
    }
    
    public Future<User> validateToken(String token) {
        if (token == null || token.trim().isEmpty()) {
            return Future.failedFuture("Token is required");
        }
        
        // Remove Bearer prefix if present
        if (token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        
        try {
            // Manual validation since Vert.x JWT auth might have issues
            JsonObject claims = extractClaims(token);
            
            // Check if token is expired
            if (isTokenExpired(token)) {
                return Future.failedFuture("Token expired");
            }
            
            // Verify signature manually
            if (isValidSignature(token)) {
                // Return a simple successful future with null user for now
                // The authentication handler will extract user info from the token directly
                return Future.succeededFuture(null);
            } else {
                return Future.failedFuture("Invalid token signature");
            }
        } catch (Exception e) {
            return Future.failedFuture("Invalid token: " + e.getMessage());
        }
    }
    
    private boolean isValidSignature(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                return false;
            }
            
            String data = parts[0] + "." + parts[1];
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(SECRET_KEY.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKeySpec);
            byte[] signature = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            String expectedSignature = Base64.getUrlEncoder().withoutPadding().encodeToString(signature);
            
            return expectedSignature.equals(parts[2]);
        } catch (Exception e) {
            return false;
        }
    }
    
    public JsonObject extractClaims(String token) {
        try {
            if (token.startsWith("Bearer ")) {
                token = token.substring(7);
            }
            
            // Decode the JWT token manually to extract claims
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                throw new IllegalArgumentException("Invalid JWT token format");
            }
            
            // Decode the payload (second part)
            byte[] payloadBytes = Base64.getUrlDecoder().decode(parts[1]);
            String payloadJson = new String(payloadBytes, StandardCharsets.UTF_8);
            return new JsonObject(payloadJson);
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to extract claims from token", e);
        }
    }
    
    public boolean isTokenExpired(String token) {
        try {
            JsonObject claims = extractClaims(token);
            Long exp = claims.getLong("exp");
            if (exp == null) {
                return true;
            }
            return Instant.now().getEpochSecond() > exp;
        } catch (Exception e) {
            return true;
        }
    }
    
    public Integer getUserIdFromToken(String token) {
        try {
            JsonObject claims = extractClaims(token);
            return claims.getInteger("userId");
        } catch (Exception e) {
            return null;
        }
    }
    
    public String getUsernameFromToken(String token) {
        try {
            JsonObject claims = extractClaims(token);
            return claims.getString("username");
        } catch (Exception e) {
            return null;
        }
    }
}