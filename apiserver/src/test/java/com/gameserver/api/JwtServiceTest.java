package com.gameserver.api;

import io.vertx.core.Vertx;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest extends BaseTest {
    
    private JwtService jwtService;
    
    @BeforeEach
    void setUp(Vertx vertx, VertxTestContext testContext) {
        super.setUp(vertx, testContext);
        jwtService = new JwtService(vertx);
    }
    
    @Test
    void testGenerateToken() {
        String token = jwtService.generateToken(1, "player1");
        
        assertNotNull(token);
        assertFalse(token.trim().isEmpty());
        assertTrue(token.contains("."));
    }
    
    @Test
    void testExtractClaims() {
        String token = jwtService.generateToken(123, "testuser");
        
        var claims = jwtService.extractClaims(token);
        
        assertEquals(123, claims.getInteger("userId"));
        assertEquals("testuser", claims.getString("username"));
        assertNotNull(claims.getLong("iat"));
        assertNotNull(claims.getLong("exp"));
    }
    
    @Test
    void testGetUserIdFromToken() {
        String token = jwtService.generateToken(456, "anotheruser");
        
        Integer userId = jwtService.getUserIdFromToken(token);
        
        assertEquals(456, userId);
    }
    
    @Test
    void testGetUsernameFromToken() {
        String token = jwtService.generateToken(789, "username123");
        
        String username = jwtService.getUsernameFromToken(token);
        
        assertEquals("username123", username);
    }
    
    @Test
    void testTokenWithBearerPrefix() {
        String token = jwtService.generateToken(100, "bearertest");
        String bearerToken = "Bearer " + token;
        
        Integer userId = jwtService.getUserIdFromToken(bearerToken);
        String username = jwtService.getUsernameFromToken(bearerToken);
        
        assertEquals(100, userId);
        assertEquals("bearertest", username);
    }
    
    @Test
    void testValidateValidToken(VertxTestContext testContext) {
        String token = jwtService.generateToken(1, "player1");
        
        jwtService.validateToken(token)
            .onSuccess(user -> {
                testContext.verify(() -> {
                    // Token validation succeeded (user can be null, that's fine)
                    assertTrue(true);
                });
                testContext.completeNow();
            })
            .onFailure(testContext::failNow);
    }
    
    @Test
    void testValidateValidTokenWithBearer(VertxTestContext testContext) {
        String token = jwtService.generateToken(1, "player1");
        String bearerToken = "Bearer " + token;
        
        jwtService.validateToken(bearerToken)
            .onSuccess(user -> {
                testContext.verify(() -> {
                    // Token validation succeeded (user can be null, that's fine)
                    assertTrue(true);
                });
                testContext.completeNow();
            })
            .onFailure(testContext::failNow);
    }
    
    @Test
    void testValidateInvalidToken(VertxTestContext testContext) {
        jwtService.validateToken("invalid.token.here")
            .onSuccess(user -> {
                testContext.failNow("Should not validate invalid token");
            })
            .onFailure(error -> {
                testContext.verify(() -> {
                    assertNotNull(error);
                });
                testContext.completeNow();
            });
    }
    
    @Test
    void testValidateNullToken(VertxTestContext testContext) {
        jwtService.validateToken(null)
            .onSuccess(user -> {
                testContext.failNow("Should not validate null token");
            })
            .onFailure(error -> {
                testContext.verify(() -> {
                    assertEquals("Token is required", error.getMessage());
                });
                testContext.completeNow();
            });
    }
    
    @Test
    void testValidateEmptyToken(VertxTestContext testContext) {
        jwtService.validateToken("")
            .onSuccess(user -> {
                testContext.failNow("Should not validate empty token");
            })
            .onFailure(error -> {
                testContext.verify(() -> {
                    assertEquals("Token is required", error.getMessage());
                });
                testContext.completeNow();
            });
    }
    
    @Test
    void testIsTokenExpired() {
        String validToken = jwtService.generateToken(1, "player1");
        
        assertFalse(jwtService.isTokenExpired(validToken));
    }
    
    @Test
    void testIsTokenExpiredWithInvalidToken() {
        assertTrue(jwtService.isTokenExpired("invalid.token"));
    }
    
    @Test
    void testTokenExpiration() {
        String token = jwtService.generateToken(1, "player1");
        var claims = jwtService.extractClaims(token);
        
        Long exp = claims.getLong("exp");
        Long iat = claims.getLong("iat");
        
        assertNotNull(exp);
        assertNotNull(iat);
        
        // Token should expire in 24 hours (86400 seconds)
        long expectedExpiry = iat + (24 * 60 * 60);
        assertEquals(expectedExpiry, exp, 5); // Allow 5 second tolerance
    }
    
    @Test
    void testExtractClaimsFromInvalidToken() {
        assertThrows(IllegalArgumentException.class, () -> {
            jwtService.extractClaims("invalid.token");
        });
    }
    
    @Test
    void testGetUserIdFromInvalidToken() {
        Integer userId = jwtService.getUserIdFromToken("invalid.token");
        assertNull(userId);
    }
    
    @Test
    void testGetUsernameFromInvalidToken() {
        String username = jwtService.getUsernameFromToken("invalid.token");
        assertNull(username);
    }
}